/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.settings;

import sirius.kernel.Sirius;
import sirius.kernel.di.FieldAnnotationProcessor;
import sirius.kernel.di.Injector;
import sirius.kernel.di.MutableGlobalContext;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.di.std.Register;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * AnnotationProcessor which handles the {@link ConfigValue} annotation.
 *
 * @see sirius.kernel.di.FieldAnnotationProcessor
 * @see ConfigValue
 */
@Register
public class ConfigValueAnnotationProcessor implements FieldAnnotationProcessor {

    @Override
    public Class<? extends Annotation> getTrigger() {
        return ConfigValue.class;
    }

    @Override
    public void handle(MutableGlobalContext ctx, Object object, Field field) throws Exception {
        ConfigValue val = field.getAnnotation(ConfigValue.class);
        String key = val.value();

        if (!Sirius.getSettings().injectValueFromConfig(object, field, key) && val.required()) {
            Injector.LOG.WARN("Missing config value: %s in (%s.%s)!",
                              key,
                              field.getDeclaringClass().getName(),
                              field.getName());
        }
    }
}
