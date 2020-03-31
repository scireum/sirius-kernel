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

        @SuppressWarnings("unchecked")
        AutoTransformer(Class<T> transformerClass) {
            AutoTransform autoTransform = transformerClass.getAnnotation(AutoTransform.class);
            this.sourceType = (Class<S>) autoTransform.value();
            this.priority = autoTransform.priority();
            this.targetType = transformerClass;
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
                return targetType.getDeclaredConstructor(sourceType).newInstance(source);
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

    @SuppressWarnings("unchecked")
    @Override
    public void handle(@Nonnull MutableGlobalContext ctx, @Nonnull Class<?> clazz) throws Exception {
        if (clazz.isAnnotationPresent(Framework.class)
            && !Sirius.isFrameworkEnabled(clazz.getAnnotation(Framework.class).value())) {
            return;
        }

        AutoTransformer<Object, Object> transformer = new AutoTransformer<>((Class<Object>) clazz);
        ctx.registerPart(transformer, Transformer.class);
    }
}
