/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.settings;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigObject;
import sirius.kernel.commons.Context;
import sirius.kernel.commons.PriorityCollector;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Value;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

/**
 * Represents an extension loaded via the {@link Settings} framework.
 */
public class Extension extends Settings implements Comparable<Extension> {

    /**
     * Name of the default entry for an extension
     */
    public static final String DEFAULT = "default";
    private static final String PRIORITY = "priority";

    protected static final Log LOG = Log.get("extensions");

    protected final int priority;
    protected final String type;
    protected final String id;
    private final ConfigObject configObject;
    private final ConfigObject defaultConfig;

    protected Extension(String type, String id, ConfigObject configObject, ConfigObject defaultConfig) {
        super(configObject.toConfig());
        this.type = type;
        this.id = id;
        this.configObject = configObject;
        this.defaultConfig = defaultConfig;
        this.priority = get(PRIORITY).asInt(PriorityCollector.DEFAULT_PRIORITY);
    }

    /**
     * Returns the {@link Value} defined for the given key.
     * <p>
     * In contrast to {@link #get(String)} this will not perform an automatic translation
     * if the value starts with a dollar sign.
     *
     * @param path the access path to retrieve the value
     * @return the value wrapping the contents for the given path. This will never by <tt>null</tt>,
     * but might be empty: {@link Value#isNull()}
     */
    @Nonnull
    public Value getRaw(String path) {
        if (configObject.containsKey(path)) {
            return Value.of(configObject.get(path).unwrapped());
        }
        if (defaultConfig != null && defaultConfig.containsKey(path)) {
            return Value.of(defaultConfig.get(path).unwrapped());
        }
        return Value.of(null);
    }

    @Override
    @Nonnull
    public Value get(String path) {
        return getRaw(path).translate();
    }

    @Nullable
    @Override
    public Config getConfig(String key) {
        if (configObject.containsKey(key)) {
            return configObject.toConfig().getConfig(key);
        }

        if (defaultConfig != null && defaultConfig.containsKey(key)) {
            return defaultConfig.toConfig().getConfig(key);
        }

        return null;
    }

    @Override
    @Nonnull
    public Context getContext() {
        Context ctx = Context.create();
        if (defaultConfig != null) {
            for (String key : defaultConfig.keySet()) {
                ctx.put(key, get(key).get());
            }
        }
        for (String key : configObject.keySet()) {
            ctx.put(key, get(key).get());
        }

        ctx.put("id", id);

        return ctx;
    }

    @Override
    public long getMilliseconds(String path) {
        try {
            return configObject.toConfig().getDuration(path, TimeUnit.MILLISECONDS);
        } catch (ConfigException.Missing e) {
            Exceptions.ignore(e);
            return defaultConfig.toConfig().getDuration(path, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw Exceptions.handle(LOG, e);
        }
    }

    /**
     * Returns the {@link Value} defined for the given key or throws a <tt>HandledException</tt> if no value was found
     * <p>
     * If this extension doesn't provide a value for this key, but there is an extension with the name
     * <tt>default</tt> which provides a value, this is used.
     * <p>
     * Returning a {@link Value} instead of a plain object provides lots of conversion methods on the one hand
     * and also guarantees a non null result on the other hand, since a <tt>Value</tt> can be empty.
     *
     * @param path the access path to retrieve the value
     * @return the value wrapping the contents for the given path. This will never by <tt>null</tt>.
     * @throws sirius.kernel.health.HandledException if no value was found for the given <tt>path</tt>
     */
    @Nonnull
    public Value require(String path) {
        Value result = get(path);
        if (result.isNull()) {
            throw Exceptions.handle()
                            .to(LOG)
                            .withSystemErrorMessage("The extension '%s' of type '%s' doesn't provide a value for: '%s'",
                                                    id,
                                                    type,
                                                    path)
                            .handle();
        }
        return result;
    }

    /**
     * Creates a new instance of the class which is named in <tt>classProperty</tt>
     * <p>
     * Tries to lookup the value for <tt>classProperty</tt>, fetches the corresponding class and creates a new
     * instance for it.
     * <p>
     * Returning a {@link Value} instead of a plain object provides lots of conversion methods on the one hand
     * and also guarantees a non null result on the other hand, since a <tt>Value</tt> can be empty.
     *
     * @param classProperty the property which is used to retrieve the class name
     * @return a new instance of the given class
     * @throws sirius.kernel.health.HandledException if no valid class was given, or if no instance could be created
     */
    @Nonnull
    public Object make(String classProperty) {
        String className = get(classProperty).asString();
        try {
            return Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
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

    /**
     * Returns the type name of the extension
     *
     * @return the name of the type, this extension was registered for
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the unique ID of this extension
     *
     * @return the map key, used to register this extension
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the complete "dot separated" name like {@code [extension.path].[id]}
     *
     * @return the complete access path used to identify this extension
     */
    public String getQualifiedName() {
        return type + "." + id;
    }

    /**
     * Returns the priority of the extension.
     * <p>
     * Defined in the config-file or the default priority defined in {@link PriorityCollector#DEFAULT_PRIORITY} if no
     * priority is defined in the config.
     *
     * @return the priority of the extension
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Determined if this extension is an artifically created default extension.
     *
     * @return <tt>true</tt> if this is an artifically created default extension used by
     * {@link ExtendedSettings#getExtension(String, String)} if nothing was found
     */
    public boolean isDefault() {
        return DEFAULT.equals(id);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Extension other = (Extension) o;
        return Strings.areEqual(id, other.id) && Strings.areEqual(type, other.type);
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(@Nullable Extension o) {
        if (o == null) {
            return -1;
        }
        int result = priority - o.priority;
        if (result == 0 && id != null && o.id != null) {
            return id.compareTo(o.id);
        } else {
            return result;
        }
    }
}
