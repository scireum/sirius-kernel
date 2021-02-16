/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.transformers;

import sirius.kernel.Sirius;
import sirius.kernel.di.ClassLoadAction;
import sirius.kernel.di.GlobalContext;
import sirius.kernel.di.MutableGlobalContext;
import sirius.kernel.di.std.Framework;
import sirius.kernel.di.std.Part;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/**
 * Responsible for processing {@link AutoTransform} annotations.
 * <p>
 * For each class wearing an <tt>AutoTransform</tt> a new {@link Transformer} is created and
 * registered in the global context.
 */
public class AutoTransformLoadAction implements ClassLoadAction {

    protected static class AutoTransformer<S, T> implements Transformer<S, T> {

        private final int priority;
        private final Class<S> sourceType;
        private final Class<T> targetType;
        private final Class<?>[] additionalTargetTypes;
        private final Class<?> transformerClass;

        @Part
        private static GlobalContext globalContext;

        public AutoTransformer(Class<?> transformerClass,
                               Class<S> sourceType,
                               Class<T> targetType,
                               Class<?>[] additionalTargetTypes,
                               int priority) {
            this.sourceType = sourceType;
            this.targetType = targetType;
            this.additionalTargetTypes = additionalTargetTypes;
            this.transformerClass = transformerClass;
            this.priority = priority;

            try {
                findConstructor();
            } catch (NoSuchMethodException e) {
                Log.SYSTEM.WARN("The class %s which is marked with @AutoTransform does neither provide a"
                                + " suitable single arg constructor nor a no-arg constructor! This will most probably"
                                + " fail at runtime!", getClass().getName());
            }
        }

        @Override
        public Class<S> getSourceClass() {
            return sourceType;
        }

        @Override
        public Class<T> getTargetClass() {
            return targetType;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Nullable
        @Override
        public T make(@Nonnull Object source) {
            try {
                T result = createInstance(source);

                if (source instanceof Composable && additionalTargetTypes != null) {
                    for (Class<?> additionalTarget : additionalTargetTypes) {
                        ((Composable) source).attach(additionalTarget, result);
                    }
                }

                globalContext.wire(result);
                return result;
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof IllegalArgumentException) {
                    return null;
                }

                throw Exceptions.handle()
                                .to(Log.SYSTEM)
                                .error(e.getCause())
                                .withSystemErrorMessage("Failed to transform %s (%s) to %s - An error occured when"
                                                        + " invoking the constructor: %s (%s)",
                                                        source,
                                                        sourceType,
                                                        targetType)
                                .handle();
            } catch (Exception e) {
                throw Exceptions.handle()
                                .to(Log.SYSTEM)
                                .error(e)
                                .withSystemErrorMessage("Failed to transform %s (%s) to %s - An error occured when"
                                                        + " invoking the constructor: %s (%s)",
                                                        source,
                                                        sourceType,
                                                        targetType)
                                .handle();
            }
        }

        @SuppressWarnings("unchecked")
        private Constructor<T> findConstructor() throws NoSuchMethodException {
            // Look for a single argument constructor with a matching parameter...
            for (Constructor<T> constructor : (Constructor<T>[]) transformerClass.getDeclaredConstructors()) {
                if (constructor.getParameterCount() == 1 && constructor.getParameterTypes()[0].isAssignableFrom(
                        sourceType)) {
                    return constructor;
                }
            }

            // ...otherwise resort to the no args constructor...
            return (Constructor<T>) transformerClass.getDeclaredConstructor();
        }

        private T createInstance(Object source)
                throws InstantiationException, IllegalAccessException, InvocationTargetException,
                       NoSuchMethodException {

            Constructor<T> constructor = findConstructor();
            if (constructor.getParameterCount() == 1) {
                return constructor.newInstance(source);
            } else {
                return constructor.newInstance();
            }
        }
    }

    @Nullable
    @Override
    public Class<? extends Annotation> getTrigger() {
        return AutoTransform.class;
    }

    @Override
    public void handle(@Nonnull MutableGlobalContext mutableContext, @Nonnull Class<?> clazz) throws Exception {
        if (clazz.isAnnotationPresent(Framework.class)
            && !Sirius.isFrameworkEnabled(clazz.getAnnotation(Framework.class).value())) {
            return;
        }

        AutoTransform autoTransform = clazz.getAnnotation(AutoTransform.class);
        Class<?>[] allTargets = mergeAllTargets(autoTransform.target(), autoTransform.targets());
        for (Class<?> target : allTargets) {
            mutableContext.registerPart(new AutoTransformer<>(clazz,
                                                              autoTransform.source(),
                                                              target,
                                                              allTargets,
                                                              autoTransform.priority()), Transformer.class);
        }
    }

    /**
     * Merge the @AutoTransform `target` with the `targets`, so both can be used.
     *
     * See also {@link AutoTransform#target()} and {@link AutoTransform#targets()}.
     * @param target the target class, or {@link Object Object.class} if none
     * @param targets the list of target classes, potentially empty
     * @return an array, containing both the targets and the target
     */
    protected Class<?>[] mergeAllTargets(Class<?> target, Class<?>[] targets) {
        if (target.equals(Object.class)) {
            // target defaults to object, and it does not make sense to transform to object in the first place
            return targets;
        }
        Class<?>[] mergedTargets = Arrays.copyOf(targets, targets.length + 1);
        mergedTargets[targets.length] = target;
        return mergedTargets;
    }
}
