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

import javax.annotation.Nullable;
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

        if (!Sirius.getSettings().injectValueFromConfig(object, field, key)
            && !field.isAnnotationPresent(Nullable.class)) {
            Injector.LOG.WARN("Cannot fill %s of %s with the config value '%s'."
                              + " Add a Nullable annotation if this is expected, in order to suppress this warning.",
                              key,
                              field.getDeclaringClass().getName(),
                              field.getName());
        }
    }
}
