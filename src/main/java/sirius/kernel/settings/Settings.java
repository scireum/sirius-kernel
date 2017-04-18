/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.settings;

import com.google.common.collect.Lists;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;
import com.typesafe.config.ConfigValueType;
import sirius.kernel.commons.Context;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Value;
import sirius.kernel.health.Exceptions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Provides a wrapper around a {@link Config} supplied by <tt>typesafe config</tt>.
 * <p>
 * Contains various boilerplate methods to safe- and quickly access the underlying config.
 */
public class Settings {

    private final Config config;

    /**
     * Creates a new wrapper for the given config.
     *
     * @param config the config to wrap
     */
    public Settings(@Nonnull Config config) {
        this.config = config;
    }

    /**
     * Provides access to the underlying config object.
     *
     * @return the underlying config object
     */
    public Config getConfig() {
        return config;
    }

    /**
     * Returns the {@link Value} defined for the given key.
     * <p>
     * If this extension doesn't provide a value for this key, but there is an extension with the name
     * <tt>default</tt> which provides a value, this is used.
     * <p>
     * If the value in the config file starts with a dollar sign, the value is treated as an i18n key and the
     * returned value will contain the translation for the current language.
     *
     * @param path the access path to retrieve the value
     * @return the value wrapping the contents for the given path. This will never by <tt>null</tt>,
     * but might be empty: {@link Value#isNull()}
     */
    @Nonnull
    public Value get(String path) {
        try {
            return Value.of(getConfig().getAnyRef(path));
        } catch (ConfigException e) {
            Exceptions.handle(e);
            return Value.EMPTY;
        }
    }

    /**
     * Returns all values defined in this extension as {@link Context}.
     *
     * @return a context containing all values defined by this extension or the <tt>default</tt> extension.
     */
    @Nonnull
    public Context getContext() {
        Context ctx = Context.create();
        for (Map.Entry<String, ConfigValue> entry : config.entrySet()) {
            ctx.put(entry.getKey(), get(entry.getKey()).get());
        }

        return ctx;
    }

    /**
     * Returns the sub config available for the given key.
     *
     * @param key name of the sub config to retrieve
     * @return the sub config for the given key or <tt>null</tt> if no such config exists.
     */
    @Nullable
    public Config getConfig(String key) {
        return getConfig().getConfig(key);
    }

    /**
     * Returns all config objects underneath the given key.
     * <p>
     * Assume we have the following config:
     * <pre>
     * test {
     *     sub {
     *         a { ... }
     *         b { ... }
     *     }
     * }
     * </pre>
     * <p>
     * Then getConfigs("sub") for the extension "test" would return a list containing a and b wrapped as config.
     * <p>
     * The name of the config object is available as "id".
     *
     * @param key the path to the config object containing a list of sub object.
     * @return a list of config object underneath the given object.
     * Returns an empty list if no matching element was found.
     */
    @Nonnull
    public List<? extends Config> getConfigs(String key) {
        List<Config> result = Lists.newArrayList();
        Config cfg = getConfig(key);
        if (cfg != null) {
            for (Map.Entry<String, ConfigValue> e : cfg.root().entrySet()) {
                if (e.getValue().valueType() == ConfigValueType.OBJECT) {
                    Config subCfg = ((ConfigObject) e.getValue()).toConfig();
                    result.add(subCfg.withValue("id", ConfigValueFactory.fromAnyRef(e.getKey())));
                }
            }
        }

        return result;
    }

    /**
     * Returns the duration in milliseconds defined for the given key.
     * <p>
     * If this extension doesn't provide a value for this key, but there is an extension with the name
     * <tt>default</tt> which provides a value, this is used.
     *
     * @param path the access path to retrieve the value
     * @return the encoded duration as milliseconds.
     * @throws sirius.kernel.health.HandledException if an invalid value was given in the config
     */
    public long getMilliseconds(String path) {
        try {
            return config.getDuration(path, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw Exceptions.handle(e);
        }
    }

    /**
     * Returns the string for the given key.
     *
     * @param key the key used to lookup the string value
     * @return the string value stored of the given key
     */
    public String getString(String key) {
        return get(key).asString();
    }

    /**
     * Returns the integer value for the given key.
     *
     * @param key the key used to lookup the value
     * @return the integer value stored in the config or 0 if an invalid value is present
     */
    public int getInt(String key) {
        return get(key).asInt(0);
    }

    public List<String> getStringList(String key) {
        return getConfig().getStringList(key);
    }

    public Map<String, String> getMap(String key) {
        Map<String, String> result = new HashMap<>();
        config.getConfig(key)
              .entrySet()
              .forEach(e -> result.put(e.getKey(), Value.of(e.getValue().unwrapped()).asString()));

        return result;
    }

    /**
     * Injects the value selected by 'key' out of the given config into the given field of the given target.
     *
     * @param target the target object to populate
     * @param field  the field to fill
     * @param key    the key to read
     * @return <tt>true</tt> if a value was present and injected, <tt>false</tt> otherwise
     */
    public boolean injectValueFromConfig(Object target, Field field, String key) {
        if (!config.hasPath(key)) {
            return false;
        }

        field.setAccessible(true);

        try {
            injectIntoField(target, field, key);

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
    private void injectIntoField(Object target, Field field, String key) throws IllegalAccessException {
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
            field.set(target, getMap(key));
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
