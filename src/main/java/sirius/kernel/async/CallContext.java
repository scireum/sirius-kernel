/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

import com.google.common.collect.Maps;
import sirius.kernel.Sirius;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Tuple;
import sirius.kernel.commons.Value;
import sirius.kernel.commons.Watch;
import sirius.kernel.health.Counter;
import sirius.kernel.health.Exceptions;
import sirius.kernel.nls.NLS;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A CallContext is attached to each thread managed by sirius.
 * <p>
 * It provides access to different sub-contexts via {@link #get(Class)}. Also, it provides acces to the mapped
 * diagnostic context (MDC). This can be filled by various parts of the framework (like which request-uri is
 * currently being processed, which user is currently active etc.) and will be attached to each error. Also, each
 * context comes with a new "flow-id". This can be used to trace an execution across different threads and even
 * across different cluster nodes.
 * <p>
 * Tasks which fork async subtasks will automatically pass on their current context. Therefore essential information
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
    private static Map<Long, CallContext> contextMap = Maps.newConcurrentMap();
    private static String nodeName = null;
    private static Counter interactionCounter = new Counter();

    /**
     * Returns the name of this computation node.
     * <p>
     * This is either the current host name or can be set via <tt>sirius.nodeName</tt>.
     *
     * @return the name of this computation node.
     */
    public static String getNodeName() {
        if (nodeName == null) {
            if (Sirius.getConfig() == null) {
                return "booting";
            }
            nodeName = Sirius.getConfig().getString("sirius.nodeName");
            if (Strings.isEmpty(nodeName)) {
                try {
                    nodeName = InetAddress.getLocalHost().getHostName();
                } catch (UnknownHostException e) {
                    Exceptions.ignore(e);
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
        CallContext ctxRef = contextMap.get(threadId);
        return Optional.ofNullable(ctxRef);
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
        CallContext ctx = getCurrentIfAvailable();
        if (ctx == null) {
            return initialize();
        }

        return ctx;
    }

    /*
     * Initializes a new context, either with a new flow-id or with the given one.
     */
    private static CallContext initialize(boolean install, String externalFlowId) {
        CallContext ctx = new CallContext();
        ctx.addToMDC(MDC_FLOW, externalFlowId);
        interactionCounter.inc();
        if (install) {
            setCurrent(ctx);
        }

        return ctx;
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
        CallContext newCtx = initialize(false, mdc.get(MDC_FLOW));
        newCtx.watch = watch;
        newCtx.mdc.put(MDC_PARENT, mdc.get(TaskContext.MDC_SYSTEM));
        subContext.entrySet().forEach(e -> newCtx.subContext.put(e.getKey(), e.getValue().fork()));
        newCtx.lang = lang;
        newCtx.fallbackLang = fallbackLang;
        return newCtx;
    }

    /**
     * Sets the CallContext for the current thread.
     *
     * @param context the context to use for the current thread.
     */
    public static void setCurrent(CallContext context) {
        currentContext.set(context);
        contextMap.put(Thread.currentThread().getId(), context);
    }

    /**
     * Detaches this CallContext from the current thread
     */
    public static void detach() {
        CallContext ctx = currentContext.get();
        if (ctx != null) {
            ctx.detachContext();
        }
        currentContext.set(null);
        contextMap.remove(Thread.currentThread().getId());
    }

    /**
     * Detaches this context from the current thread.
     * <p>
     * This will notify all sub contexts ({@link SubContext}) that this context essentially ended.
     */
    public void detachContext() {
        for (SubContext ctx : subContext.values()) {
            try {
                ctx.detach();
            } catch (Exception e) {
                Exceptions.handle()
                          .error(e)
                          .withSystemErrorMessage("Error detaching sub context '%s': %s (%s)", ctx.getClass().getName())
                          .handle();
            }
        }
    }

    private Map<String, String> mdc = Maps.newLinkedHashMap();

    /*
     * Needs to be synchronized as a CallContext might be shared across several sub tasks
     */
    private Map<Class<? extends SubContext>, SubContext> subContext = Collections.synchronizedMap(Maps.newHashMap());
    private Watch watch = Watch.start();
    private String lang = NLS.getDefaultLanguage();
    private String fallbackLang;

    /**
     * Returns the current mapped diagnostic context (MDC).
     *
     * @return a list of name-value pair representing the current mdc.
     */
    public List<Tuple<String, String>> getMDC() {
        return Tuple.fromMap(mdc);
    }

    /**
     * Returns the value of the named variable in the mdc.
     *
     * @param key the name of the variable to read.
     * @return the value of the mapped diagnostic context.
     */
    public Value getMDCValue(String key) {
        return Value.of(mdc.get(key));
    }

    /**
     * Returns the Watch representing the execution time.
     *
     * @return a Watch, representing the duration since the creation of the <b>CallContext</b>. Due to CallContexts
     * being passed to forked sub tasks, the returned duration can be longer than the execution time within the
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
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public <C extends SubContext> C get(Class<C> contextType) {
        try {
            SubContext result = subContext.get(contextType);
            if (result == null) {
                result = contextType.newInstance();
                subContext.put(contextType, result);
            }

            return (C) result;
        } catch (Exception e) {
            throw Exceptions.handle()
                            .error(e)
                            .withSystemErrorMessage("Cannot get instance of %s from current CallContext: %s (%s)",
                                                    contextType.getName())
                            .handle();
        }
    }

    /**
     * Installs the given sub context.
     * <p>
     * This should only be used if required (e.g. in test environments to replace/mock objects). Otherwise a
     * call to {@link #get(Class)} will initialize the requested sub context.
     *
     * @param contextType the type of the context to set
     * @param instance    the instance to set
     * @param <C>         the type of the sub-context
     */
    public <C extends SubContext> void set(Class<C> contextType, C instance) {
        subContext.put(contextType, instance);
    }

    /**
     * Returns the current language determined for the current thread.
     *
     * @return a two-letter language code used for the current thread.
     */
    public String getLang() {
        return lang;
    }

    /**
     * Returns the current fallback language determined for the current thread.
     *
     * @return a two-letter language code used for the current thread.
     */
    @Nullable
    public String getFallbackLang() {
        return fallbackLang;
    }

    /**
     * Sets the current language for the current thread.
     * <p>
     * If <tt>null</tt> or an empty string is passed in, the language will not be changed.
     * </p>
     *
     * @param lang the two-letter language code for this thread.
     */
    public void setLang(@Nullable String lang) {
        if (Strings.isFilled(lang)) {
            this.lang = lang;
        }
    }

    /**
     * Sets the current fallback language for the current thread.
     *
     * @param fallbackLang the two-letter language code for this thread.
     */
    public void setFallbackLang(@Nullable String fallbackLang) {
        this.fallbackLang = fallbackLang;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : mdc.entrySet()) {
            sb.append(e.getKey());
            sb.append(": ");
            sb.append(e.getValue());
            sb.append("\n");
        }

        return sb.toString();
    }
}
