/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.cache;

/**
 * Permits to implement a cache coherence protocol.
 * <p>
 * {@link CoherentCache Coherent caches} created using {@link CacheManager#createCoherentCache(String)} will notify
 * an instace which is {@link sirius.kernel.di.std.Register registered} for this interface once a key needs to be
 * removed or if the cache is entirely cleared.
 * <p>
 * The coherence implementation must broadcast this information to all nodes of a cluster and call
 * {@link CacheManager#clearCoherentCacheLocally(String)} or
 * {@link CacheManager#removeCoherentCacheKeyLocally(String, String)} locally on each node.
 */
public interface CacheCoherence {

    /**
     * Notifies the coherence manager that the given cache needs to be cleared on all nodes, including the calling one.
     *
     * @param cache the cache to clear
     */
    void clear(CoherentCache<?> cache);

    /**
     * Notifies the coherence manager that the given key from the given cache needs to be removed on all nodes,
     * including the calling one.
     *
     * @param cache the cache to remove the key (value) from
     * @param key   the key to remove
     */
    void removeKey(CoherentCache<?> cache, String key);
}
