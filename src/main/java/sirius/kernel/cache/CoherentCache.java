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
import java.util.function.Predicate;

/**
 * Provides a cache implementation which supports cache coherence by roaming <tt>clear</tt> and <tt>remove</tt> calls.
 *
 * @param <V> the type of the values supported by this cache
 */
class CoherentCache<V> extends ManagedCache<String, V> {
    /**
     * Creates a new cache. This is not intended to be called outside of <tt>CacheManager</tt>.
     *
     * @param name          name of the cache which is also used to fetch the config settings
     * @param valueComputer used to compute absent cache values for given keys. May be null.
     * @param verifier      used to verify cached values before they are delivered to the caller.
     */
    protected CoherentCache(String name,
                            @Nullable ValueComputer<String, V> valueComputer,
                            @Nullable ValueVerifier<V> verifier) {
        super(name, valueComputer, verifier);
    }

    @Override
    public void clear() {
        CacheManager.clearCoherentCache(this);
    }

    /**
     * Invoked by {@link CacheManager#clearCoherentCacheLocally(String)} to clear this cache on this node.
     */
    public void clearLocal() {
        super.clear();
    }

    @Override
    public void remove(String key) {
        CacheManager.removeCoherentCacheKey(this, key);
    }

    /**
     * Invoked by {@link CacheManager#removeCoherentCacheKeyLocally(String, String)} to remove the given key from
     * the given cache locally.
     *
     * @param key the key to remove from this cache
     */
    public void removeLocal(String key) {
        super.remove(key);
    }

    @Override
    public void removeIf(@Nonnull Predicate<CacheEntry<String, V>> predicate) {
        if (data == null) {
            return;
        }

        data.asMap().values().stream().filter(predicate).map(CacheEntry::getKey).forEach(this::remove);
    }
}
