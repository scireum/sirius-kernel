/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.std;

import sirius.kernel.di.FieldAnnotationProcessor;
import sirius.kernel.di.MutableGlobalContext;
import sirius.kernel.extensions.Extensions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Handles the {@link ExtensionList} annotation.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @see sirius.kernel.di.FieldAnnotationProcessor
 * @see ExtensionList
 * @since 2013/08
 */
@Register
public class ExtensionListAnnotationProcessor implements FieldAnnotationProcessor {
    @Override
    public Class<? extends Annotation> getTrigger() {
        return ExtensionList.class;
    }

    @Override
    public void handle(MutableGlobalContext ctx, Object object, Field field) throws Exception {
        ExtensionList val = field.getAnnotation(ExtensionList.class);
        field.set(object, Extensions.getExtensions(val.value()));
    }

}
