/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di;

import com.google.common.collect.Lists;
import sirius.kernel.Classpath;
import sirius.kernel.commons.Callback;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Central class for collecting and injecting dependencies (which are called <b>parts</b>).
 * <p>
 * Contains the micro kernel which keeps track of all parts and injects them where required. We consider this
 * a micro kernel, as it doesn't directly know any annotations. It rather delegates the work to classes which implement
 * defined interfaces. Therefore the implementation is rather short but easily extensible.
 * </p>
 * <p>
 * Parts are commonly added via subclasses of {@link ClassLoadAction}. These scan each class in the classpath
 * and instantiate and insert them into the {@link MutableGlobalContext} if required. Most of these
 * <tt>ClassLoadAction</tt> implementations trigger on annotations. A prominent example is the
 * {@link sirius.kernel.di.std.AutoRegisterAction} which loads all classes wearing the {@link sirius.kernel.di.std.Register}
 * annotation. Subclasses of <tt>ClassLoadAction</tt> are discovered automatically.
 * </p>
 * <p>
 * Accessing parts can be done in two ways. First, one can access the current {@link GlobalContext} via
 * {@link #context()}. This can be used to retrieve parts by class or by class and name. The second way
 * to access parts is to use marker annotations like {@link sirius.kernel.di.std.Part},
 * {@link sirius.kernel.di.std.Parts} or {@link sirius.kernel.di.std.Context}. Again,
 * these annotations are not processed by the micro kernel itself, but by subclasses of {@link FieldAnnotationProcessor}.
 * To process all annotations of a given Java object, {@link GlobalContext#wire(Object)} can be used. This will
 * be automatically called for each part which is auto-instantiated by a <tt>ClassLoadAction</tt>.
 * </p>
 * <p>
 * Also all annotations on static fields are processed on system startup. This is a simple trick to pass a
 * part to objects which are frequently created and destroyed.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public class Injector {

    /**
     * Logger used by the injection framework. This is public so that annotation processors in sub packages can
     * log through this logger.
     */
    public static final Log LOG = Log.get("di");

    private static PartRegistry ctx = new PartRegistry();
    private static List<Class<?>> loadedClasses;

    /**
     * Initializes the framework. Must be only called once on system startup.
     *
     * @param callback  the given callback is invoked, once the system is initialized and permits to add external
     *                  parts to the given context.
     * @param classpath the classpath used to enumerate all classes to be scanned
     */
    public static void init(@Nullable Callback<MutableGlobalContext> callback, @Nonnull final Classpath classpath) {
        ctx = new PartRegistry();

        loadedClasses = Lists.newArrayList();
        final List<ClassLoadAction> actions = new ArrayList<ClassLoadAction>();
        LOG.INFO("Initializing the MicroKernel....");

        LOG.INFO("~ Scanning .class files...");
        classpath.find(Pattern.compile(".*?\\.class")).forEach(matcher -> {
            String relativePath = matcher.group();
            String className = relativePath.substring(0, relativePath.length() - 6).replace("/", ".");
            try {
                LOG.FINE("Found class: " + className);
                Class<?> clazz = Class.forName(className, true, classpath.getLoader());
                if (ClassLoadAction.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
                    try {
                        actions.add((ClassLoadAction) clazz.newInstance());
                    } catch (Throwable e) {
                        Exceptions.handle()
                                .error(e)
                                .to(LOG)
                                .withSystemErrorMessage("Failed to instantiate ClassLoadAction: %s - %s (%s)",
                                        className)
                                .handle();
                    }
                }
                loadedClasses.add(clazz);
            } catch (NoClassDefFoundError e) {
                Exceptions.handle()
                        .error(e)
                        .to(LOG)
                        .withSystemErrorMessage("Failed to load dependent class: %s", className)
                        .handle();
            } catch (Throwable e) {
                Exceptions.handle()
                        .error(e)
                        .to(LOG)
                        .withSystemErrorMessage("Failed to load class %s: %s (%s)", className)
                        .handle();
            }
        });

        LOG.INFO("~ Applying %d class load actions on %d classes...", actions.size(), loadedClasses.size());
        for (Class<?> clazz : loadedClasses) {
            for (ClassLoadAction action : actions) {
                if (action.getTrigger() == null || clazz.isAnnotationPresent(action.getTrigger())) {
                    LOG.FINE("Auto-installing class: %s based on %s", clazz.getName(), action.getClass().getName());
                    try {
                        action.handle(ctx, clazz);
                    } catch (Throwable e) {
                        Exceptions.handle()
                                .error(e)
                                .to(LOG)
                                .withSystemErrorMessage("Failed to auto-load: %s with ClassLoadAction: %s: %s (%s)",
                                        clazz.getName(),
                                        action.getClass().getSimpleName())
                                .handle();
                    }
                }
            }
        }

        LOG.INFO("~ Enhancing context...");
        if (callback != null) {
            try {
                callback.invoke(ctx);
            } catch (Exception e) {
                LOG.SEVERE(e);
            }
        }

        LOG.INFO("~ Initializing static parts-references...");
        for (Class<?> clazz : loadedClasses) {
            ctx.wireClass(clazz);
        }

        LOG.INFO("~ Initializing parts...");
        ctx.processAnnotations();
    }

    /**
     * Provides access to the global context, containing all parts
     * <p>
     * This can also be loaded into a class field using the {@link sirius.kernel.di.std.Context} annotation
     * </p>
     *
     * @return the global context containing all parts known to the system
     */
    public static GlobalContext context() {
        return ctx;
    }

    /**
     * Returns a list of all loaded classes.
     *
     * @return a list of all classes detected at system startup
     */
    public static List<Class<?>> getAllLoadedClasses() {
        return Collections.unmodifiableList(loadedClasses);
    }

}
