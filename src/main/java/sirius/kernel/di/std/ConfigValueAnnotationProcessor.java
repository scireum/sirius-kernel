/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.std;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueType;
import sirius.kernel.Sirius;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Value;
import sirius.kernel.di.FieldAnnotationProcessor;
import sirius.kernel.di.Injector;
import sirius.kernel.di.MutableGlobalContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
        Config config = Sirius.getConfig();

        if (!injectValueFromConfig(object, field, key, config) && val.required()) {
            Injector.LOG.WARN("Missing config value: %s in (%s.%s)!",
                              key,
                              field.getDeclaringClass().getName(),
                              field.getName());
        }
    }

    /**
     * Injects the value selected by 'key' out of the given config into the given field of the given target.
     *
     * @param target the target object to populate
     * @param field  the field to fill
     * @param key    the key to read
     * @param config the config object to read from
     * @return <tt>true</tt> if a value was present and injected, <tt>false</tt> otherwise
     */
    public static boolean injectValueFromConfig(Object target, Field field, String key, Config config) {
        if (!config.hasPath(key)) {
            return false;
        }

        field.setAccessible(true);

        try {
            injectIntoField(target, field, key, config);

            return true;
        } catch (IllegalAccessException e) {
            // This should not happen, as we set the field to be accessible
            throw new IllegalArgumentException(Strings.apply("Cannot fill field '%s.%s' of type %s with a config value!",
                                                             field.getDeclaringClass().getName(),
                                                             field.getName(),
                                                             field.getType().getName()), e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void injectIntoField(Object target, Field field, String key, Config config)
            throws IllegalAccessException {
        if (String.class.equals(field.getType())) {
            field.set(target, config.getString(key));
        } else if (int.class.equals(field.getType()) || Integer.class.equals(field.getType())) {
            field.set(target, config.getInt(key));
        } else if (long.class.equals(field.getType()) || Long.class.equals(field.getType())) {
            if (config.getValue(key).valueType() == ConfigValueType.NUMBER) {
                field.set(target, config.getLong(key));
            } else {
                field.set(target, config.getBytes(key));
            }
        } else if (boolean.class.equals(field.getType()) || Boolean.class.equals(field.getType())) {
            field.set(target, config.getBoolean(key));
        } else if (List.class.equals(field.getType())) {
            field.set(target, config.getStringList(key));
        } else if (Map.class.equals(field.getType())) {
            Map<String, String> result = new HashMap<>();
            config.getConfig(key)
                  .entrySet()
                  .forEach(e -> result.put(e.getKey(), Value.of(e.getValue().unwrapped()).asString()));
            field.set(target, result);
        } else if (Duration.class.equals(field.getType())) {
            field.set(target, Duration.ofMillis(config.getDuration(key, TimeUnit.MILLISECONDS)));
        } else if (field.getType().isEnum()) {
            field.set(target, Value.of(config.getString(key)).asEnum((Class<? extends Enum>) field.getType()));
        } else {
            throw new IllegalArgumentException(Strings.apply("Cannot fill field '%s.%s' of type %s with a config value!",
                                                             field.getDeclaringClass().getName(),
                                                             field.getName(),
                                                             field.getType().getName()));
        }
    }
}
