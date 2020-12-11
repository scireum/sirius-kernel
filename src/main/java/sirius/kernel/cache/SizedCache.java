/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import sirius.kernel.Sirius;
import sirius.kernel.health.Exceptions;

import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * Defines a size limited mapping from key to values.
 * <p>
 * Items can be put in the cache by direct method or calculated from a value computer, either a default
 * one provided when initializing the class and/or a custom one when getting an item.
 * <p>
 * The maximum size of the cache is given during initialization, either with a fixed value or by providing
 * a configuration key where the value should be retrieved from.
 * <p>
 * Note that a {@link ManagedCache} is the preferred solution for most cases, but certain situations might
 * require a pure local cache, such as a temporary big data load where cache polution is not desired.
 * In such a case, this class will give the comfort and easy of use Map-like implementation eliminating
 * the risk of an out-of-memory.
 *
 * @param <T> the type of object to be stored in the cache
 */
public class SizedCache<T> {

    private final LoadingCache<String, T> cache;
    private final boolean hasDefaultLoader;

    /**
     * Creates a sized cache with a maximum size and default value computer.
     *
     * @param maximumSize          the maximum size of the cache
     * @param defaultValueComputer the default function called to compute the value of a missing entry
     */
    public SizedCache(long maximumSize, Function<String, T> defaultValueComputer) {
        this.cache = CacheBuilder.newBuilder().maximumSize(maximumSize).build(new CacheLoader<String, T>() {
            @Override
            public T load(String key) throws Exception {
                if (defaultValueComputer == null) {
                    return null;
                }
                return defaultValueComputer.apply(key);
            }
        });
        this.hasDefaultLoader = (defaultValueComputer != null);
    }

    /**
     * Creates a sized cache with a maximum size and default value computer.
     *
     * @param configKey            the name of a configuration key containing the maximum size of the cache
     * @param defaultValueComputer the default function called to compute the value of a missing entry
     */
    public SizedCache(String configKey, Function<String, T> defaultValueComputer) {
        this(obtainSizeFromConfig(configKey), defaultValueComputer);
    }

    /**
     * Creates a sized cache with a maximum size.
     *
     * @param maximumSize the maximum size of the cache
     */
    public SizedCache(long maximumSize) {
        this(maximumSize, null);
    }

    /**
     * Creates a sized cache with a maximum size.
     *
     * @param configKey the name of a configuration key containing the maximum size of the cache
     */
    public SizedCache(String configKey) {
        this(obtainSizeFromConfig(configKey), null);
    }

    private static long obtainSizeFromConfig(String configKey) {
        long maximumSize = Sirius.getSettings().getInt(configKey);
        if (maximumSize == 0) {
            throw Exceptions.createHandled()
                            .withSystemErrorMessage("Cannot initialize a sized cache from configuration key '%s'.",
                                                    configKey)
                            .handle();
        }
        return maximumSize;
    }

    /**
     * Retrieves the value for a given key.
     * <p>
     * This method throws an {@link IllegalStateException} if no default value provider has been initialized.
     *
     * @param key the key to search
     * @return the object associated with the key
     */
    public T get(String key) {
        if (!hasDefaultLoader) {
            throw new IllegalStateException("Cache does not provide a default loader to compute data.");
        }
        return cache.getUnchecked(key);
    }

    /**
     * Retrieves the value for a given key.
     *
     * @param key           the key to search
     * @param valueComputer the function called to compute the value of a missing entry
     * @return the object associated with the key
     */
    public T get(String key, Function<String, T> valueComputer) {
        try {
            return cache.get(key, () -> valueComputer.apply(key));
        } catch (ExecutionException e) {
            throw Exceptions.createHandled()
                            .error(e)
                            .withSystemErrorMessage("Cannot compute value for key '%s' in sized cache.", key)
                            .handle();
        }
    }

    /**
     * Checks if the given key is currently loaded in the cache.
     *
     * @param key the key to search
     * @return <tt>true</tt> if the key is loaded in the cache, <tt>false</tt> otherwise
     */
    public boolean containsKey(String key) {
        return (cache.getIfPresent(key) != null);
    }

    /**
     * Inserts or replaces a value in the cache for the given key.
     *
     * @param key   the key to search
     * @param value the object to be inserted or replaced for the provided key
     */
    public void put(String key, T value) {
        cache.put(key, value);
    }

    /**
     * Clears all items loaded in the cache.
     */
    public void clear() {
        cache.invalidateAll();
    }
}
