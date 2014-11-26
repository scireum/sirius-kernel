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
 * Can be supplied to {@link CacheManager#createCache(String, ValueComputer, ValueVerifier)} when creating a cache
 * to verify values before returning them to the caller.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
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
