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
import sirius.kernel.di.MutableGlobalContext;
import sirius.kernel.di.std.Framework;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;

/**
 * Responsible for processing {@link AutoTransform} annotations.
 * <p>
 * For each class wearing an <tt>AutoTransform</tt> a new {@link Transformer} is created and
 * registered in the global context.
 */
public class AutoTransformLoadAction implements ClassLoadAction {

    private static class AutoTransformer<S, T> implements Transformer<S, T> {

        private int priority;
        private Class<S> sourceType;
        private Class<T> targetType;
        private Class<?> transformerClass;

        @SuppressWarnings("unchecked")
        AutoTransformer(Class<?> transformerClass) {
            AutoTransform autoTransform = transformerClass.getAnnotation(AutoTransform.class);
            this.sourceType = (Class<S>) autoTransform.source();
            this.targetType = (Class<T>) autoTransform.target();
            this.transformerClass = transformerClass;
            this.priority = autoTransform.priority();
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

        @SuppressWarnings("unchecked")
        @Nullable
        @Override
        public T make(@Nonnull Object source) {
            try {
                return (T) transformerClass.getDeclaredConstructor(sourceType).newInstance(source);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof IllegalArgumentException) {
                    return null;
                }

                throw Exceptions.handle()
                                .to(Log.SYSTEM)
                                .error(e.getCause())
                                .withSystemErrorMessage(
                                        "Failed to transform %s (%s) to %s - An error occured when invoking the constructor: %s (%s)",
                                        source,
                                        sourceType,
                                        targetType)
                                .handle();
            } catch (Exception e) {
                throw Exceptions.handle()
                                .to(Log.SYSTEM)
                                .error(e)
                                .withSystemErrorMessage(
                                        "Failed to transform %s (%s) to %s - An error occured when invoking the constructor: %s (%s)",
                                        source,
                                        sourceType,
                                        targetType)
                                .handle();
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

        ctx.registerPart(new AutoTransformer<>(clazz), Transformer.class);
    }
}
