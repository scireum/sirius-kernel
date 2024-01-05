/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

import sirius.kernel.Sirius;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Tuple;
import sirius.kernel.commons.Value;
import sirius.kernel.commons.Watch;
import sirius.kernel.health.Counter;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;
import sirius.kernel.nls.NLS;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A CallContext is attached to each thread managed by sirius.
 * <p>
 * It provides access to different sub-contexts via {@link #get(Class)}. Also, it provides access to the mapped
 * diagnostic context (MDC). This can be filled by various parts of the framework (like which request-uri is
 * currently being processed, which user is currently active etc.) and will be attached to each error. Also, each
 * context comes with a new "flow-id". This can be used to trace an execution across different threads and even
 * across different cluster nodes.
 * <p>
 * Tasks which fork async subtasks will automatically pass on their current context. Therefore, essential information
 * can be passed along, without having to provide a method parameter for each value. Since sub-contexts can be of any
 * type, this concept can be enhanced by additional frameworks or application programs.
 */
@ParametersAreNonnullByDefault
public class CallContext {

    /**
     * Name of the flow variable in the MDC.
     */
    public static final String MDC_FLOW = "flow";

    /**
     * Name of the parent context in the MDC
     */
    public static final String MDC_PARENT = "parent";

    private static final ThreadLocal<CallContext> currentContext = new ThreadLocal<>();

    private static final Map<Long, CallContext> contextMap = new ConcurrentHashMap<>();
    private static String nodeName = null;
    private static final Counter interactionCounter = new Counter();

    private final Map<String, Object> mdc = new ConcurrentHashMap<>();

    /*
     * Needs to be synchronized as a CallContext might be shared across several sub-tasks.
     */
    private final Map<Class<? extends SubContext>, SubContext> subContexts =
            Collections.synchronizedMap(new HashMap<>());
    private Watch watch = Watch.start();
    private String language;
    private Consumer<CallContext> lazyLanguageInstaller;
    private String fallbackLanguage;

    /**
     * Returns the name of this computation node.
     * <p>
     * This is either the current host name or can be set via <tt>sirius.nodeName</tt>.
     *
     * @return the name of this computation node.
     */
    public static String getNodeName() {
        if (nodeName == null) {
            if (Sirius.getSettings() == null) {
                return "booting";
            }
            nodeName = Sirius.getSettings().getString("sirius.nodeName");
            if (Strings.isEmpty(nodeName)) {
                try {
                    nodeName = InetAddress.getLocalHost().getHostName();
                } catch (UnknownHostException exception) {
                    Exceptions.ignore(exception);
                    Tasks.LOG.WARN(Strings.apply(
                            "Cannot determine hostname - consider setting 'sirius.nodeName' in the configuration."));
                    nodeName = "unknown";
                }
            }
        }

        return nodeName;
    }

    /**
     * Returns the <tt>CallContext</tt> for the given thread or an empty optional if none is present.
     *
     * @param threadId the id of the thread to fetch the <tt>CallContext</tt> for
     * @return the CallContext for the given thread wrapped as optional
     */
    @Nonnull
    public static Optional<CallContext> getContext(long threadId) {
        CallContext context = contextMap.get(threadId);
        return Optional.ofNullable(context);
    }

    /**
     * Returns the context for the current thread.
     *
     * @return the current context or <tt>null</tt> if none is present yet
     */
    @Nullable
    public static CallContext getCurrentIfAvailable() {
        return currentContext.get();
    }

    /**
     * Returns the context for the current thread.
     * <p>
     * If no context is available, a new one will be initialized.
     *
     * @return the <tt>CallContext</tt> of the current thread.
     */
    @Nonnull
    public static CallContext getCurrent() {
        CallContext context = getCurrentIfAvailable();
        if (context == null) {
            return initialize();
        }

        return context;
    }

    /*
     * Initializes a new context, either with a new flow-id or with the given one.
     */
    private static CallContext initialize(boolean install, String externalFlowId) {
        CallContext context = new CallContext();
        context.addToMDC(MDC_FLOW, externalFlowId);
        interactionCounter.inc();
        if (install) {
            setCurrent(context);
        }

        return context;
    }

    /**
     * Provides access to the interaction counter.
     * <p>
     * This counts all CallContexts which have been created and is used
     * to provide rough system utilization metrics.
     *
     * @return the Counter, which contains the total number of CallContexts created
     */
    public static Counter getInteractionCounter() {
        return interactionCounter;
    }

    /**
     * Creates a new CallContext for the given thread.
     * <p>
     * Discards the current <tt>CallContext</tt>, if there was already one.
     *
     * @return the newly created CallContext, which is already attached to the current thread.
     */
    public static CallContext initialize() {
        return initialize(true, getNodeName() + "/" + interactionCounter.getCount());
    }

    /**
     * Forks and creates a sub context.
     * <p>
     * All instantiated sub contexts are forked, the MDC is re-initialized.
     *
     * @return a copy of the current call context including a fork of all available sub contexts.
     * @see SubContext#fork()
     */
    public CallContext fork() {
        CallContext forkedContext = initialize(false, getMDCValue(MDC_FLOW).asString());
        forkedContext.watch = watch;
        forkedContext.addToMDC(MDC_PARENT, getMDCValue(TaskContext.MDC_SYSTEM).asString());
        subContexts.forEach((key, value) -> forkedContext.subContexts.put(key, value.fork()));
        forkedContext.language = language;
        forkedContext.lazyLanguageInstaller = lazyLanguageInstaller;
        forkedContext.fallbackLanguage = fallbackLanguage;
        return forkedContext;
    }

    /**
     * Sets the CallContext for the current thread.
     *
     * @param context the context to use for the current thread.
     */
    public static void setCurrent(CallContext context) {
        currentContext.set(context);
        contextMap.put(Thread.currentThread().threadId(), context);
    }

    /**
     * Detaches this CallContext from the current thread.
     */
    public static void detach() {
        CallContext context = currentContext.get();
        if (context != null) {
            context.detachContext();
        }
        currentContext.remove();
        contextMap.remove(Thread.currentThread().threadId());
    }

    /**
     * Detaches this context from the current thread.
     * <p>
     * This will notify all sub contexts ({@link SubContext}) that this context essentially ended.
     */
    public void detachContext() {
        for (SubContext subContext : subContexts.values()) {
            try {
                subContext.detach();
            } catch (Exception exception) {
                Exceptions.handle()
                          .error(exception)
                          .withSystemErrorMessage("Error detaching sub context '%s': %s (%s)",
                                                  subContext.getClass().getName())
                          .handle();
            }
        }
    }

    /**
     * Returns the current mapped diagnostic context (MDC).
     *
     * @return a list of name-value pair representing the current mdc.
     */
    public List<Tuple<String, String>> getMDC() {
        List<Tuple<String, String>> result = new ArrayList<>();
        for (Map.Entry<String, Object> entry : mdc.entrySet()) {
            if (entry.getValue() instanceof Supplier) {
                result.add(Tuple.create(entry.getKey(), Value.of(((Supplier<?>) entry.getValue()).get()).asString()));
            } else {
                result.add(Tuple.create(entry.getKey(), String.valueOf(entry.getValue())));
            }
        }

        return result;
    }

    /**
     * Returns the value of the named variable in the mdc.
     *
     * @param key the name of the variable to read.
     * @return the value of the mapped diagnostic context.
     */
    public Value getMDCValue(String key) {
        Object data = mdc.get(key);
        if (data instanceof Supplier) {
            return Value.of(((Supplier<?>) data).get());
        } else {
            return Value.of(data);
        }
    }

    /**
     * Returns the Watch representing the execution time.
     *
     * @return a Watch, representing the duration since the creation of the <b>CallContext</b>. Due to CallContexts
     * being passed to forked sub-tasks, the returned duration can be longer than the execution time within the
     * current thread.
     */
    public Watch getWatch() {
        return watch;
    }

    /**
     * Adds a value to the mapped diagnostic context.
     *
     * @param key   the name of the value to add
     * @param value the value to add to the mdc.
     */
    public void addToMDC(String key, @Nullable String value) {
        mdc.put(key, value == null ? "" : value);
    }

    /**
     * Adds a value to the mapped diagnostic context.
     *
     * @param key   the name of the value to add
     * @param value the supplier to add to the mdc. Will be evaluated one the MDC is used elsewhere.
     */
    public void addToMDC(String key, @Nullable Supplier<String> value) {
        mdc.put(key, value == null ? "" : value);
    }

    /**
     * Removes the value of the mdc for key.
     *
     * @param key the name of the value to remove.
     */
    public void removeFromMDC(String key) {
        mdc.remove(key);
    }

    /**
     * Returns or creates the sub context of the given type.
     * <p>
     * The class of the sub context must provide a no-args constructor, as it will be instantiated if non existed.
     *
     * @param contextType the type of the sub-context to be returned.
     * @param <C>         the type of the sub-context
     * @return an instance of the given type. If no instance was available, a new one is created
     * @deprecated use {@link #getOrCreateSubContext(Class)} instead.
     */
    @Nonnull
    @Deprecated(since = "2023-01-04", forRemoval = true)
    public <C extends SubContext> C get(@Nonnull Class<C> contextType) {
        return getOrCreateSubContext(contextType);
    }

    /**
     * Returns or creates the sub context of the given type.
     * <p>
     * The class of the sub context must provide a no-args constructor, as it will be instantiated if non existed.
     *
     * @param contextType the type of the sub-context to be returned.
     * @param <C>         the type of the sub-context
     * @return an instance of the given type. If no instance was available, a new one is created
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public <C extends SubContext> C getOrCreateSubContext(@Nonnull Class<C> contextType) {
        try {
            SubContext result = subContexts.get(contextType);
            if (result == null) {
                result = contextType.getDeclaredConstructor().newInstance();
                subContexts.put(contextType, result);
            }

            return (C) result;
        } catch (Exception exception) {
            throw Exceptions.handle()
                            .error(exception)
                            .withSystemErrorMessage("Cannot get instance of %s from current CallContext: %s (%s)",
                                                    contextType.getName())
                            .handle();
        }
    }

    /**
     * Installs the given sub context.
     * <p>
     * This should only be used if required (e.g. in test environments to replace/mock objects). Otherwise, a
     * call to {@link #get(Class)} will initialize the requested sub context.
     *
     * @param contextType the type of the context to set
     * @param instance    the instance to set
     * @param <C>         the type of the sub-context
     * @deprecated use {@link #setSubContext(Class, SubContext)} instead.
     */
    @Deprecated(since = "2023-01-04", forRemoval = true)
    public <C extends SubContext> void set(@Nonnull Class<C> contextType, @Nonnull C instance) {
        setSubContext(contextType, instance);
    }

    /**
     * Installs the given sub context.
     * <p>
     * This should only be used if required (e.g. in test environments to replace/mock objects). Otherwise, a
     * call to {@link #getOrCreateSubContext(Class)} will initialize the requested sub context.
     *
     * @param contextType the type of the context to set
     * @param instance    the instance to set
     * @param <C>         the type of the sub-context
     */
    public <C extends SubContext> void setSubContext(@Nonnull Class<C> contextType, @Nonnull C instance) {
        subContexts.put(contextType, instance);
    }

    /**
     * Returns the sub context of the given type.
     * <p>
     * Note: In contrast to {@link #getOrCreateSubContext(Class)}, this method will not create a new instance if none is present.
     *
     * @param contextType the type of the sub-context to be returned
     * @param <C>         the type of the sub-context
     * @return an instance of the given type or <tt>null</tt> if no instance was available
     */
    @SuppressWarnings("unchecked")
    public <C extends SubContext> Optional<C> tryGetSubContext(@Nonnull Class<C> contextType) {
        return Optional.ofNullable((C) subContexts.get(contextType));
    }

    /**
     * Determines if a sub context of the given type is present.
     *
     * @param contextType the type of the sub-context to be returned
     * @param <C>         the type of the sub-context
     * @return <tt>true</tt> if a sub context of the given type is present
     */
    public <C extends SubContext> boolean hasSubContext(@Nonnull Class<C> contextType) {
        return subContexts.containsKey(contextType) && subContexts.get(contextType) != null;
    }

    /**
     * Removes the sub context of the given type.
     *
     * @param contextType the type of the sub-context to be removed
     * @param <C>         the type of the sub-context
     */
    public <C extends SubContext> void removeSubContext(@Nonnull Class<C> contextType) {
        subContexts.remove(contextType);
    }

    /**
     * Returns the current language determined for the current thread.
     *
     * @return a two-letter language code used for the current thread.
     */
    public String getLanguage() {
        if (language == null) {
            invokeLazyLanguageInstaller();
            if (language == null) {
                language = NLS.getDefaultLanguage();
            }
        }

        return language;
    }

    /**
     * Returns the current language determined for the current thread.
     *
     * @return a two-letter language code used for the current thread.
     * @deprecated call {@link #getLanguage()} instead.
     */
    @Deprecated
    public final String getLang() {
        return getLanguage();
    }

    private void invokeLazyLanguageInstaller() {
        // We obtain a local copy and set the field to null, to avoid a circular
        // execution in case the language installer itself accesses getLang...
        Consumer<CallContext> localCopy = this.lazyLanguageInstaller;
        if (localCopy != null) {
            this.lazyLanguageInstaller = null;
            try {
                localCopy.accept(this);
            } catch (Exception exception) {
                Exceptions.handle()
                          .to(Log.SYSTEM)
                          .withSystemErrorMessage("An error occurred while computing the current language: %s (%s)")
                          .handle();
            }
        }
    }

    /**
     * Returns the current fallback language determined for the current thread.
     *
     * @return a two-letter language code used for the current thread.
     */
    @Nullable
    public String getFallbackLanguage() {
        return fallbackLanguage;
    }

    /**
     * Returns the current fallback language determined for the current thread.
     *
     * @return a two-letter language code used for the current thread.
     * @deprecated call {@link #getFallbackLang()} instead.
     */
    @Nullable
    @Deprecated
    public final String getFallbackLang() {
        return getFallbackLanguage();
    }

    /**
     * Sets the current language for the current thread.
     * <p>
     * If <tt>null</tt> or an empty string is passed in, the language will not be changed.
     * </p>
     *
     * @param language the two-letter language code for this thread.
     */
    public void setLanguage(@Nullable String language) {
        if (Strings.isFilled(language)) {
            this.language = language;
            this.lazyLanguageInstaller = null;
        }
    }

    /**
     * Sets the current language for the current thread.
     * <p>
     * If <tt>null</tt> or an empty string is passed in, the language will not be changed.
     * </p>
     *
     * @param language the two-letter language code for this thread.
     * @deprecated call {@link #setLanguage(String)} instead.
     */
    @Deprecated
    public final void setLang(@Nullable String language) {
        setLanguage(language);
    }

    /**
     * Sets the current language for the current thread, only if no other language has been set already.
     * <p>
     * If <tt>null</tt> or an empty string is passed in, the language will not be changed.
     * </p>
     *
     * @param language the two-letter language code for this thread.
     */
    public void setLanguageIfEmpty(@Nullable String language) {
        if (Strings.isEmpty(this.language)) {
            setLanguage(language);
        }
    }

    /**
     * Sets the current language for the current thread, only if no other language has been set already.
     * <p>
     * If <tt>null</tt> or an empty string is passed in, the language will not be changed.
     * </p>
     *
     * @param language the two-letter language code for this thread.
     * @deprecated call {@link #setLanguageIfEmpty(String)} instead.
     */
    @Deprecated
    public final void setLangIfEmpty(@Nullable String language) {
        setLanguageIfEmpty(language);
    }

    /**
     * Adds a language supplier.
     * <p>
     * In certain circumstances the current language might be influenced by something which is hard to compute.
     * For example a web request in <b>sirius-web</b> might either provide a user with a language attached via
     * its session, or it might contain a language header which itself isn't quite easy to parse.
     * <p>
     * Worst of all, in many cases, the current language might not be used at all.
     * <p>
     * Therefore, we permit lazy computations which are only evaluated as required.
     *
     * @param languageInstaller a callback which installs the appropriate language into the given call context
     *                          (is passed again to support {@link #fork() forking}).
     */
    public void deferredSetLanguage(@Nonnull Consumer<CallContext> languageInstaller) {
        this.language = null;
        this.lazyLanguageInstaller = languageInstaller;
    }

    /**
     * Adds a language supplier.
     * <p>
     * In certain circumstances the current language might be influenced by something which is hard to compute.
     * For example a web request in <b>sirius-web</b> might either provide a user with a language attached via
     * its session, or it might contain a language header which itself isn't quite easy to parse.
     * <p>
     * Worst of all, in many cases, the current language might not be used at all.
     * <p>
     * Therefore, we permit lazy computations which are only evaluated as required.
     *
     * @param languageInstaller a callback which installs the appropriate language into the given call context
     *                          (is passed again to support {@link #fork() forking}).
     * @deprecated call {@link #deferredSetLanguage(Consumer)} instead.
     */
    @Deprecated
    public final void deferredSetLang(@Nonnull Consumer<CallContext> languageInstaller) {
        deferredSetLanguage(languageInstaller);
    }

    /**
     * Sets the language back to <tt>null</tt>.
     * <p>
     * This method should only be used to re-initialize the language. Use {@link #setLanguage(String)} to specify a new language.
     */
    public void resetLanguage() {
        language = null;
    }

    /**
     * Sets the lang back to <tt>null</tt>.
     * <p>
     * This method should only be used to re-initialize the language. Use {@link #setLanguage(String)} to specify a new language.
     *
     * @deprecated call {@link #resetLanguage()} instead.
     */
    @Deprecated
    public final void resetLang() {
        resetLanguage();
    }

    /**
     * Sets the current fallback language for the current thread.
     *
     * @param fallbackLanguage the two-letter language code for this thread.
     */
    public void setFallbackLanguage(@Nullable String fallbackLanguage) {
        this.fallbackLanguage = fallbackLanguage;
    }

    /**
     * Sets the current fallback language for the current thread.
     *
     * @param fallbackLanguage the two-letter language code for this thread.
     * @deprecated call {@link #setFallbackLanguage(String)} instead.
     */
    @Deprecated
    public final void setFallbackLang(@Nullable String fallbackLanguage) {
        setFallbackLanguage(fallbackLanguage);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Tuple<String, String> entry : getMDC()) {
            builder.append(entry.getFirst());
            builder.append(": ");
            builder.append(entry.getSecond());
            builder.append("\n");
        }

        return builder.toString();
    }
}
