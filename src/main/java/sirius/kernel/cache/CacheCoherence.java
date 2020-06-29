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
    void clear(Cache<String, ?> cache);

    /**
     * Notifies the coherence manager that the given key from the given cache needs to be removed on all nodes,
     * including the calling one.
     *
     * @param cache the cache to remove the key (value) from
     * @param key   the key to remove
     */
    void removeKey(Cache<String, ?> cache, String key);

    /**
     * Notifies the coherence manager that the appropriate delete handler should be invoked on all nodes,
     * including the calling one.
     *
     * @param cache         the cache to remove from
     * @param discriminator the discriminator used to select the appropriate delete handler
     * @param testInput     the input used by the predicate in order to determine which entities to remove
     */
    void removeAll(Cache<String, ?> cache, String discriminator, String testInput);
}
