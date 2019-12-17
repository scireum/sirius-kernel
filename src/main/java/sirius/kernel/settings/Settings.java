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
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;
import com.typesafe.config.ConfigValueType;
import sirius.kernel.Sirius;
import sirius.kernel.commons.Context;
import sirius.kernel.commons.Explain;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Value;
import sirius.kernel.di.std.Priorized;
import sirius.kernel.health.Exceptions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Provides a wrapper around a {@link Config} supplied by <tt>typesafe config</tt>.
 * <p>
 * Contains various boilerplate methods to safe- and quickly access the underlying config.
 * <p>
 * Note that this class will (in normal use-cases) never throws an exception. However, if it is created as
 * <tt>strict</tt>, it will log an error if a requested config path does not exist. An example of a string settings
 * instance is the system configuration found in {@link Sirius#getSettings()}. Most user specific configs will most
 * probably be non-strict.
 */
@ParametersAreNonnullByDefault
public class Settings {

    private static final String PRIORITY = "priority";
    private static final String ID = "id";

    private final Config config;
    private final boolean strict;

    /**
     * Creates a new wrapper for the given config.
     *
     * @param config the config to wrap
     * @param strict determines if the config is strict. A strict config will log an error if an unkown path is
     *               requested
     */
    public Settings(Config config, boolean strict) {
        this.config = config;
        this.strict = strict;
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
     * Note that if this config is <tt>strict</tt>, an error is logged if the requested path does not exist. However,
     * no exception will be thrown.
     *
     * @param path the access path to retrieve the value
     * @return the value wrapping the contents for the given path. This will never be <tt>null</tt>,
     * but might be empty: {@link Value#isNull()}
     */
    @Nonnull
    public Value get(String path) {
        try {
            return Value.of(getConfig().getAnyRef(path));
        } catch (ConfigException e) {
            if (strict) {
                Exceptions.handle(e);
            }

            return Value.EMPTY;
        }
    }

    /**
     * Determines if the requested path exists in the underlying config.
     *
     * @param path the path to check
     * @return <tt>true</tt> if the requested path exists, <tt>false</tt> otherwise
     */
    public boolean has(String path) {
        return getConfig().hasPath(path);
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
        try {
            return getConfig().getConfig(key);
        } catch (Exception e) {
            if (strict) {
                Exceptions.handle(e);
            }

            return ConfigFactory.empty();
        }
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
     * <p>
     * The list will be sorted by "priority". If no explicit priority is given, we try to sort elements along their
     * natural order within the file. If multiple files are merged together, the behaviour of this approach is
     * undefined and <tt>priority</tt> should be used.
     *
     * @param key the path to the config object containing a list of sub object.
     * @return a list of config objects underneath the given object or an empty list if there are none
     */
    @Nonnull
    public List<? extends Config> getConfigs(String key) {
        List<Config> result = Lists.newArrayList();
        Config cfg = getConfig(key);
        if (cfg != null) {
            for (Map.Entry<String, ConfigValue> e : cfg.root().entrySet()) {
                if (e.getValue().valueType() == ConfigValueType.OBJECT) {
                    Config subCfg = ((ConfigObject) e.getValue()).toConfig();
                    result.add(subCfg.withValue(ID, ConfigValueFactory.fromAnyRef(e.getKey())));
                }
            }
        }

        result.sort((a, b) -> {
            int prioA = a.hasPath(PRIORITY) ? a.getInt(PRIORITY) : Priorized.DEFAULT_PRIORITY;
            int prioB = b.hasPath(PRIORITY) ? b.getInt(PRIORITY) : Priorized.DEFAULT_PRIORITY;

            if (prioA == prioB) {
                prioA = a.origin().lineNumber();
                prioB = b.origin().lineNumber();
            }

            return prioA - prioB;
        });

        return result;
    }

    /**
     * Returns the duration in milliseconds defined for the given key.
     * <p>
     * If no config is present at the given path, or it can not be converted to a duration, 0 is returned.
     *
     * @param path the access path to retrieve the value
     * @return the duration as milliseconds
     */
    public long getMilliseconds(String path) {
        try {
            return config.getDuration(path, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            if (strict) {
                Exceptions.handle(e);
            }
            return 0;
        }
    }

    /**
     * Returns the string for the given key.
     *
     * @param key the key used to lookup the string value
     * @return the string value stored of the given key
     */
    @Nonnull
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

    /**
     * Returns the list of strings for the given key.
     *
     * @param key the key used to lookup the value
     * @return a list of strings stored of the given key
     */
    @Nonnull
    public List<String> getStringList(String key) {
        try {
            return getConfig().getStringList(key);
        } catch (ConfigException e) {
            if (strict) {
                Exceptions.handle(e);
            }
            return Collections.emptyList();
        }
    }

    /**
     * Reads a embedded map.
     *
     * @param key the key of the map to read
     * @return the map within the config
     */
    public Map<String, String> getMap(String key) {
        Map<String, String> result = new HashMap<>();
        getConfig(key).entrySet().forEach(e -> result.put(e.getKey(), Value.of(e.getValue().unwrapped()).asString()));

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

    @SuppressWarnings({"unchecked", "rawtypes", "squid:S3776", "squid:MethodCyclomaticComplexity"})
    @Explain("This is the shortest and most efficient way to check all those types.")
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
        } else if (Set.class.equals(field.getType())) {
            field.set(target, new HashSet<>(config.getStringList(key)));
        } else if (float.class.equals(field.getType()) || Float.class.equals(field.getType())) {
            field.set(target, config.getNumber(key).floatValue());
        } else if (double.class.equals(field.getType()) || Double.class.equals(field.getType())) {
            field.set(target, config.getNumber(key).doubleValue());
        } else {
            throw new IllegalArgumentException(Strings.apply("Cannot fill field '%s.%s' of type %s with a config value!",
                                                             field.getDeclaringClass().getName(),
                                                             field.getName(),
                                                             field.getType().getName()));
        }
    }
}
