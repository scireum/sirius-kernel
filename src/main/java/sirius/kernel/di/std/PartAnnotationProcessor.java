/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.std;

import sirius.kernel.Sirius;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.FieldAnnotationProcessor;
import sirius.kernel.di.MutableGlobalContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * Handles the {@link Part} annotation.
 *
 * @see sirius.kernel.di.FieldAnnotationProcessor
 * @see Part
 */
@Register
public class PartAnnotationProcessor implements FieldAnnotationProcessor {
    @Override
    public Class<? extends Annotation> getTrigger() {
        return Part.class;
    }

    @Override
    public void handle(MutableGlobalContext ctx, Object object, Field field) throws Exception {
        Part annotation = field.getAnnotation(Part.class);
        if (Strings.isFilled(annotation.configPath())) {
            String value = Sirius.getSettings().getString(annotation.configPath());
            if (Strings.isFilled(value)) {
                Object part = ctx.findPart(value, field.getType());
                if (part != null) {
                    field.set(object, part);
                }
            }
        } else {
            Object part = ctx.getPart(field.getType());
            if (part != null) {
                field.set(object, part);
            }
        }
    }
}
