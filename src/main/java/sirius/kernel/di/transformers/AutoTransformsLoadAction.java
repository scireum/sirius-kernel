/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.transformers;

import sirius.kernel.Sirius;
import sirius.kernel.di.MutableGlobalContext;
import sirius.kernel.di.std.Framework;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;

/**
 * Responsible for processing repeated {@link AutoTransform} annotations with type {@link AutoTransform.List}.
 * <p>
 * For each class and each annotation of type <tt>AutoTransform</tt>  a new {@link Transformer} is created and
 * registered in the global context.
 */
public class AutoTransformsLoadAction extends AutoTransformLoadAction {

    @Nullable
    @Override
    public Class<? extends Annotation> getTrigger() {
        return AutoTransform.List.class;
    }

    @Override
    public void handle(@Nonnull MutableGlobalContext ctx, @Nonnull Class<?> clazz) throws Exception {
        if (clazz.isAnnotationPresent(Framework.class)
            && !Sirius.isFrameworkEnabled(clazz.getAnnotation(Framework.class).value())) {
            return;
        }

        AutoTransform[] autoTransforms = clazz.getAnnotationsByType(AutoTransform.class);
        for (AutoTransform autotransForm : autoTransforms) {
            ctx.registerPart(new AutoTransformer<>(clazz, autotransForm), Transformer.class);
        }
    }
}
