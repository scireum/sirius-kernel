/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.std;

import com.typesafe.config.ConfigValueType;
import sirius.kernel.Sirius;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.FieldAnnotationProcessor;
import sirius.kernel.di.Injector;
import sirius.kernel.di.MutableGlobalContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;

/**
 * AnnotationProcessor which handles the {@link ConfigValue} annotation.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @see sirius.kernel.di.FieldAnnotationProcessor
 * @see ConfigValue
 * @since 2013/08
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
        if (Sirius.getConfig().hasPath(val.value())) {
            if (String.class.equals(field.getType())) {
                field.set(object, Sirius.getConfig().getString(val.value()));
            } else if (int.class.equals(field.getType()) || Integer.class.equals(field.getType())) {
                field.set(object, Sirius.getConfig().getInt(val.value()));
            } else if (long.class.equals(field.getType()) || Long.class.equals(field.getType())) {
                if (Sirius.getConfig().getValue(val.value()).valueType() == ConfigValueType.NUMBER) {
                    field.set(object, Sirius.getConfig().getLong(val.value()));
                } else {
                    field.set(object, Sirius.getConfig().getBytes(val.value()));
                }
            } else if (boolean.class.equals(field.getType()) || Boolean.class.equals(field.getType())) {
                field.set(object, Sirius.getConfig().getBoolean(val.value()));
            } else if (List.class.equals(field.getType())) {
                field.set(object, Sirius.getConfig().getStringList(val.value()));
            } else if (Duration.class.equals(field.getType())) {
                field.set(object, Duration.ofMillis(Sirius.getConfig().getMilliseconds(val.value())));
            } else {
                throw new IllegalArgumentException(Strings.apply("Cannot fill field of type %s with a config value!",
                                                                 field.getType().getName()));
            }
        } else if (val.required()) {
            Injector.LOG.WARN("Missing config value: %s in (%s.%s)!",
                              val.value(),
                              field.getDeclaringClass().getName(),
                              field.getName());
        }
    }

}
