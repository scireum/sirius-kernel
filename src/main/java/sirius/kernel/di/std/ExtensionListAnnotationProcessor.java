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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * Handles the {@link ExtensionList} annotation.
 *
 * @see sirius.kernel.di.FieldAnnotationProcessor
 * @see ExtensionList
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
