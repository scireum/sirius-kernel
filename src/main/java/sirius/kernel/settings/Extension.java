/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.settings;

import com.typesafe.config.ConfigObject;
import sirius.kernel.Sirius;
import sirius.kernel.async.ExecutionPoint;
import sirius.kernel.commons.Context;
import sirius.kernel.commons.Explain;
import sirius.kernel.commons.PriorityCollector;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Value;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;
import sirius.kernel.nls.NLS;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;

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

    @SuppressWarnings("java:S2259")
    @Explain("There is a check for null in the withFallback method, so this is safe.")
    protected Extension(String type, String id, ConfigObject configObject, ConfigObject defaultConfig) {
        super(withFallback(configObject, defaultConfig).toConfig(), false);
        this.configObject = withFallback(configObject, defaultConfig);
        this.type = type;
        this.id = id;
        this.priority = get(PRIORITY).asInt(PriorityCollector.DEFAULT_PRIORITY);
    }

    private static ConfigObject withFallback(@Nullable ConfigObject config, @Nullable ConfigObject fallback) {
        if (config == null && fallback == null) {
            throw Exceptions.handle()
                            .to(LOG)
                            .withSystemErrorMessage("Cannot create an extension without a config or fallback")
                            .handle();
        }
        if (config == null || config.isEmpty()) {
            return fallback;
        } else if (fallback == null || fallback.isEmpty()) {
            return config;
        } else {
            return config.withFallback(fallback);
        }
    }

    @Override
    @Nonnull
    public Value get(String path) {
        // This method will be removed soon so that the original behaviour of Settings.get takes its place.
        // Automatic translation will then be replaced by manual using getTranslatedString.
        Value value = getRaw(path);
        if (value.isFilled() && value.is(String.class)) {
            if (!Sirius.isProd() && value.asString().startsWith("$") && !value.startsWith("${")) {
                Log.SYSTEM.WARN(
                        "Extension.get with automatic translation was used for %s of %s for key %s\n%s\n\nThis has been deprecated. use getTranslatedString as automatic translation will be disabled.",
                        getId(),
                        getType(),
                        path,
                        ExecutionPoint.snapshot());
            }

            return Value.of(NLS.smartGet(value.asString(), null));
        }

        return value;
    }

    @Override
    @Nonnull
    public Context getContext() {
        Context ctx = Context.create();
        for (String key : configObject.keySet()) {
            ctx.put(key, get(key).get());
        }

        ctx.put("id", id);

        return ctx;
    }

    /**
     * Returns the {@link Value} defined for the given key or throws a <tt>HandledException</tt> if no value was found
     * <p>
     * If this extension doesn't provide a value for this key, but there is an extension with the name
     * <tt>default</tt> which provides a value, this is used.
     * <p>
     * Returning a {@link Value} instead of a plain object provides lots of conversion methods on the one hand
     * and also guarantees a non-null result on the other hand, since a <tt>Value</tt> can be empty.
     *
     * @param path the access path to retrieve the value
     * @return the value wrapping the contents for the given path. This will never be <tt>null</tt>.
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
     * Tries to look up the value for <tt>classProperty</tt>, fetches the corresponding class and creates a new
     * instance for it.
     * <p>
     * Returning a {@link Value} instead of a plain object provides lots of conversion methods on the one hand
     * and also guarantees a non-null result on the other hand, since a <tt>Value</tt> can be empty.
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
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException exception) {
            throw Exceptions.handle()
                            .error(exception)
                            .to(LOG)
                            .withSystemErrorMessage(
                                    "Cannot create instance of class %s (%s) for extension %s of type %s: %s (%s)",
                                    className,
                                    classProperty,
                                    id,
                                    type)
                            .handle();
        } catch (ClassNotFoundException exception) {
            throw Exceptions.handle()
                            .error(exception)
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
     * Determined if this extension is an artificially created default extension.
     *
     * @return <tt>true</tt> if this is an artificially created default extension used by
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
