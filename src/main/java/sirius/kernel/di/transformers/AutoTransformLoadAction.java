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

        private int priority;
        private Class<S> sourceType;
        private Class<T> targetType;
        private Class<?>[] additionalTargetTypes;
        private Class<?> transformerClass;

        @Part
        private static GlobalContext globalContext;

        @SuppressWarnings("unchecked")
        AutoTransformer(Class<?> transformerClass,
                        AutoTransform autoTransform,
                        Class<T> targetType,
                        Class<?>[] additionalTargetTypes) {
            this.sourceType = (Class<S>) autoTransform.source();
            this.targetType = targetType;
            this.additionalTargetTypes = additionalTargetTypes;
            this.transformerClass = transformerClass;
            this.priority = autoTransform.priority();

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
    public void handle(@Nonnull MutableGlobalContext ctx, @Nonnull Class<?> clazz) throws Exception {
        if (clazz.isAnnotationPresent(Framework.class)
            && !Sirius.isFrameworkEnabled(clazz.getAnnotation(Framework.class).value())) {
            return;
        }

        AutoTransform autoTransform = clazz.getAnnotation(AutoTransform.class);
        Class<?>[] allTargets = mergeAllTargets(autoTransform);
        Class<?>[] targets = autoTransform.targets();
        for (Class<?> target : targets) {
            ctx.registerPart(new AutoTransformer<>(clazz, autoTransform, target, allTargets), Transformer.class);
        }
        Class<?> target = autoTransform.target();
        if (!target.equals(Object.class)) {
            ctx.registerPart(new AutoTransformer<>(clazz, autoTransform, target, allTargets), Transformer.class);
        }
    }

    private Class<?>[] mergeAllTargets(AutoTransform autoTransform) {
        Class<?> target = autoTransform.target();
        if (!target.equals(Object.class)) {
            Class<?>[] targets = autoTransform.targets();
            Class<?>[] mergedTargets = Arrays.copyOf(targets, targets.length + 1);
            mergedTargets[targets.length] = target;
            return mergedTargets;
        }
        return autoTransform.targets();
    }
}
