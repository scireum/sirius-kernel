/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.extensions;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;
import com.typesafe.config.ConfigValueType;
import sirius.kernel.Sirius;
import sirius.kernel.commons.Context;
import sirius.kernel.commons.PriorityCollector;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Value;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Extension system based on the system configuration, providing simple access to extension lists.
 * <p>
 * Using the <a href="https://github.com/typesafehub/config" target="_blank">typesafe config library</a>,
 * several component.conf (along with application.conf etc.) will be loaded to form the system config
 * (see: {@link sirius.kernel.Sirius#setupConfiguration()}). These files will be merged together, so that extensions
 * like the following wil be put into one extension list reachable as "examples":
 * <pre>
 * {@code
 *     File A:
 *
 *      examples {
 *          A = {
 *              key = "string"
 *              otherKey = true
 *          }
 *      }
 *
 *     File B:
 *
 *      examples {
 *          B = {
 *              key = "string"
 *              otherKey = true
 *          }
 *      }
 * }
 * </pre>
 * <p>
 * This permits frameworks to provide extension hooks which can be extended by sub modules, without having the
 * framework to "know" those modules. Using a loose coupled approach like this simplifies the task of building
 * modular and extensible systems.
 * <p>
 * The extensions defined above can be obtained calling {@code Extensions.getExtension("examples")}. Each
 * of those extensions can be read out calling {@code ext.getValue("key").asString()} or
 * {@code ext.getValue("otherKey").asBoolean()}
 * <p>
 * Another way of loading extensions is to place an {@link sirius.kernel.di.std.ExtensionList} annotation
 * on a field like:
 * <pre>
 * {@code
 *     &#64;ExtensionList("examples")
 *     private List&lt;Extension&gt; examples;
 * }
 * </pre>
 * This will be detected by the {@link sirius.kernel.di.std.ExtensionListAnnotationProcessor} as the
 * {@link sirius.kernel.di.Injector} starts up and filled with the appropriate list.
 *
 * @see Extension
 * @see Value
 * @see sirius.kernel.Sirius#setupConfiguration()
 */
@ParametersAreNonnullByDefault
public class Extensions {

    /**
     * Name of the default entry for an extension
     */
    public static final String DEFAULT = "default";

    /**
     * The logger used by the extensions framework
     */
    protected static final Log LOG = Log.get("extensions");

    /*
     * Class must not be instantiated. All methods are static
     */
    private Extensions() {
    }

    /*
     * Used as cache for already loaded extension lists
     */
    private static Map<String, Map<String, Extension>> cache = Maps.newConcurrentMap();

    /*
     * Used as cache for the default values of a given extension type
     */
    private static Map<String, Extension> defaultsCache = Maps.newConcurrentMap();

    /**
     * Returns the <tt>Extension</tt> for the given <tt>id</tt> of the given <tt>type</tt>
     *
     * @param type the type of the extension to be returned
     * @param id   the unique id of the extension to be returned
     * @return the specified extension or <tt>null</tt>, if no such extension exists
     */
    @Nullable
    public static Extension getExtension(String type, String id) {
        if (!id.matches("[a-z0-9\\-]+")) {
            LOG.WARN("Bad extension id detected: '%s' (for type: %s). Names should only consist of lowercase letters,"
                     + " "
                     + "digits or '-'", id, type);
        }

        Extension result = getExtensionMap(type).get(id);
        if (result == null) {
            return getDefault(type);
        }

        return result;
    }

    private static Extension getDefault(String type) {
        Extension result = defaultsCache.get(type);
        if (result != null) {
            return result;
        }
        ConfigObject cfg = Sirius.getConfig().getConfig(type).root();
        ConfigObject def = (ConfigObject) cfg.get(DEFAULT);
        if (cfg.containsKey(DEFAULT)) {
            result = new BasicExtension(type, DEFAULT, def, null);
            defaultsCache.put(type, result);
            return result;
        }

        return null;
    }

    private static Map<String, Extension> getExtensionMap(String type) {
        Map<String, Extension> result = cache.get(type);
        if (result != null) {
            return result;
        }
        if (Sirius.getConfig() == null || !Sirius.getConfig().hasPath(type)) {
            return Collections.emptyMap();
        }
        ConfigObject cfg = Sirius.getConfig().getConfig(type).root();
        List<BasicExtension> list = Lists.newArrayList();
        ConfigObject def = null;
        if (cfg.containsKey(DEFAULT)) {
            def = (ConfigObject) cfg.get(DEFAULT);
        }
        for (Map.Entry<String, ConfigValue> entry : cfg.entrySet()) {
            String key = entry.getKey();
            if (!DEFAULT.equals(key) && !key.contains(".")) {
                if (entry.getValue() instanceof ConfigObject) {
                    list.add(new BasicExtension(type, key, (ConfigObject) entry.getValue(), def));
                } else {
                    LOG.WARN("Malformed extension within '%s'. Expected a config object but found: %s",
                             type,
                             entry.getValue());
                }
            }
        }
        Collections.sort(list);
        result = Maps.newLinkedHashMap();
        for (Extension ex : list) {
            result.put(ex.getId(), ex);
        }
        cache.put(type, result);
        return result;
    }

    /**
     * Returns all extensions available for the given type
     * <p>
     * The order of the extensions can be defined, setting a property named <tt>priority</tt>. If no value is
     * present {@link PriorityCollector#DEFAULT_PRIORITY} is assumed.
     *
     * @param type the type of the extensions to be returned.
     * @return a non-null collection of extensions found for the given type
     */
    @Nonnull
    public static Collection<Extension> getExtensions(String type) {
        return getExtensionMap(type).values();
    }

    /**
     * Default implementation of <tt>Extension</tt>.
     */
    static class BasicExtension implements Extension, Comparable<BasicExtension> {

        private final int priority;
        private String type;
        private final String id;
        private ConfigObject config;
        private ConfigObject def;

        protected BasicExtension(String type, String key, ConfigObject config, @Nullable ConfigObject def) {
            this.type = type;
            this.id = key;
            this.config = config;
            this.def = def;
            this.priority = get("priority").asInt(PriorityCollector.DEFAULT_PRIORITY);
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public boolean isDefault() {
            return DEFAULT.equals(id);
        }

        @Override
        public String getQualifiedName() {
            return type + "." + id;
        }

        @Override
        @Nonnull
        public Value get(String path) {
            if (config.containsKey(path)) {
                return Value.of(config.get(path).unwrapped()).translate();
            }
            if (def != null && def.containsKey(path)) {
                return Value.of(def.get(path).unwrapped()).translate();
            }
            return Value.of(null);
        }

        @Nullable
        @Override
        public Config getConfig(String key) {
            if (config.containsKey(key)) {
                return config.toConfig().getConfig(key);
            }
            if (def != null && def.containsKey(key)) {
                return config.toConfig().getConfig(key);
            }
            return null;
        }

        @Override
        @Nonnull
        public Context getContext() {
            Context ctx = Context.create();
            if (def != null) {
                for (String key : def.keySet()) {
                    ctx.put(key, get(key).get());
                }
            }
            for (String key : config.keySet()) {
                ctx.put(key, get(key).get());
            }

            ctx.put("id", id);

            return ctx;
        }

        @SuppressWarnings("Convert2streamapi")
        @Nonnull
        @Override
        public List<Config> getConfigs(String key) {
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

        @Override
        public long getMilliseconds(String path) {
            try {
                return config.toConfig().getMilliseconds(path);
            } catch (ConfigException.Missing e) {
                Exceptions.ignore(e);
                return def.toConfig().getMilliseconds(path);
            } catch (Exception e) {
                throw Exceptions.handle(e);
            }
        }

        @Override
        @Nonnull
        public Value require(String path) {
            Value result = get(path);
            if (result.isNull()) {
                throw Exceptions.handle()
                                .to(LOG)
                                .withSystemErrorMessage(
                                        "The extension '%s' of type '%s' doesn't provide a value for: '%s'",
                                        id,
                                        type,
                                        path)
                                .handle();
            }
            return result;
        }

        @Override
        @Nonnull
        public Object make(String classProperty) {
            String className = require(classProperty).asString();
            try {
                return Class.forName(className).newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw Exceptions.handle()
                                .error(e)
                                .to(LOG)
                                .withSystemErrorMessage(
                                        "Cannot create instance of class %s (%s) for extension %s of type %s: %s (%s)",
                                        className,
                                        classProperty,
                                        id,
                                        type)
                                .handle();
            } catch (ClassNotFoundException e) {
                throw Exceptions.handle()
                                .error(e)
                                .to(LOG)
                                .withSystemErrorMessage("Class %s not found for %s in extension %s of type %s",
                                                        className,
                                                        classProperty,
                                                        id,
                                                        type)
                                .handle();
            }
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            BasicExtension that = (BasicExtension) o;
            return Strings.areEqual(id, that.id) && Strings.areEqual(type, that.type);
        }

        @Override
        public int hashCode() {
            int result = type != null ? type.hashCode() : 0;
            result = 31 * result + (id != null ? id.hashCode() : 0);
            return result;
        }

        @Override
        public int compareTo(@Nullable BasicExtension o) {
            if (o == null) {
                return -1;
            }
            return priority - o.priority;
        }
    }
}
