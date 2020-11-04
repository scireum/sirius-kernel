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
import sirius.kernel.health.Exceptions;

import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public class SizedCache<T> {

    private final LoadingCache<String, T> cache;
    private final boolean hasDefaultLoader;

    public SizedCache(long maximumSize, Function<String, T> defaultLoader) {
        this.cache = CacheBuilder.newBuilder().maximumSize(maximumSize).build(new CacheLoader<String, T>() {
            @Override
            public T load(String key) throws Exception {
                return defaultLoader.apply(key);
            }
        });
        this.hasDefaultLoader = true;
    }

    public SizedCache(long maximumSize) {
        this(maximumSize, key -> null);
    }

    public T get(String key) {
        if (!hasDefaultLoader) {
            throw new IllegalStateException("Cache does not provide a default loader to compute data.");
        }
        return cache.getUnchecked(key);
    }

    public T get(String key, Function<String, T> loader) {
        try {
            return cache.get(key, () -> loader.apply(key));
        } catch (ExecutionException e) {
            throw Exceptions.createHandled()
                            .error(e)
                            .withSystemErrorMessage("Cannot compute value for a sized cache.")
                            .handle();
        }
    }

    public void put(String key, T value) {
        cache.put(key, value);
    }

    public void clear() {
        cache.invalidateAll();
    }
}
