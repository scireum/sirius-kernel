/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.cache;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Computes a value if it is not found in a cache
 * <p>
 * Can be supplied to {@link CacheManager#createCache(String, ValueComputer, ValueVerifier)} when creating a cache
 * to compute values which are not found in the cache.
 *
 * @param <K> the key type determining the type of the lookup values in the cache
 * @param <V> the value type determining the type of values stored in the cache
 */
public interface ValueComputer<K, V> {

    /**
     * Computes the value for the given key
     *
     * @param key the key which was used to lookup a value in the cache
     * @return the appropriate value to be cached for the given key
     */
    @Nullable
    V compute(@Nonnull K key);
}
