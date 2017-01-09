/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.std;

import com.google.common.collect.Lists;
import sirius.kernel.di.FieldAnnotationProcessor;
import sirius.kernel.di.MutableGlobalContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;

/**
 * Handles the {@link sirius.kernel.di.std.PriorityParts} annotation.
 *
 * @see sirius.kernel.di.FieldAnnotationProcessor
 * @see sirius.kernel.di.std.PriorityParts
 */
@Register
public class PriorityPartsAnnotationProcessor implements FieldAnnotationProcessor {
    @Override
    public Class<? extends Annotation> getTrigger() {
        return PriorityParts.class;
    }

    @Override
    public void handle(MutableGlobalContext ctx, Object object, Field field) throws Exception {
        PriorityParts parts = field.getAnnotation(PriorityParts.class);
        if (Collection.class.isAssignableFrom(field.getType())) {
            if (!Priorized.class.isAssignableFrom(parts.value())) {
                throw new IllegalArgumentException(
                        "PriorityParts annotations may only be used with classes implementing 'Priorized'");
            }
            List<Priorized> result = Lists.newArrayList(ctx.getParts(parts.value()));
            result.sort((o1, o2) -> {
                if (o1 == o2) {
                    return 0;
                }
                if (o2 == null) {
                    return -1;
                }
                if (o1 == null) {
                    return 1;
                }
                return o1.getPriority() - o2.getPriority();
            });
            field.set(object, result);
        } else {
            throw new IllegalArgumentException(
                    "Only fields of type Collection or List are allowed whe using @PriorityParts.");
        }
    }
}
