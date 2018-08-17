/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.cache;

import javax.annotation.Nullable;

/**
 * Checks if a cached value is still valid
 * <p>
 * Can be supplied to {@link CacheManager#createLocalCache(String, ValueComputer, ValueVerifier)} or
 * {@link CacheManager#createCoherentCache(String, ValueComputer, ValueVerifier)} when creating a cache to verify
 * values before returning them to the caller.
 *
 * @param <V> the value type determining the type of values stored in the cache
 */
public interface ValueVerifier<V> {

    /**
     * Verifies the given value
     *
     * @param value the value to be verified
     * @return <tt>true</tt> if the value is still valid, <tt>false</tt> otherwise
     */
    boolean valid(@Nullable V value);
}
