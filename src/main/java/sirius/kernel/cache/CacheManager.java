/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.cache;

import sirius.kernel.Stoppable;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Provides access to all managed caches
 * <p>
 * Is responsible for creating new caches using {@link #createLocalCache(String)} or {@link #createCoherentCache(String)}.
 * Also, this class keeps track of all known caches.
 * <p>
 * Additionally instances of {@link InlineCache} can be created, which can be used to compute a single value,
 * which is then cached for a given amount of time.
 */
public class CacheManager {

    /**
     * Logged used by the caching system
     */
    protected static final Log LOG = Log.get("cache");

    /**
     * Lists all known caches.
     */
    private static Map<String, ManagedCache<?, ?>> caches = new ConcurrentHashMap<>();

    private static final Duration INLINE_CACHE_DEFAULT_TTL = Duration.ofSeconds(10);

    @Part
    @Nullable
    private static CacheCoherence cacheCoherence;

    /**
     * This class has only static members and is not intended to be instantiated
     */
    private CacheManager() {
    }

    /**
     * Returns a sorted list of all known caches
     *
     * @return a sorted list of all caches created so far
     */
    public static List<ManagedCache<?, ?>> getCaches() {
        return caches.values().stream().sorted().collect(Collectors.toList());
    }

    /**
     * Creates a cache with the given name which is only managed locally.
     * <p>
     * The name is used to load the settings from the system configuration, using the extension <tt>cache.[name]</tt>.
     * If a value is absent in the cache, the given <tt>valueComputer</tt> is used to generate the requested value. If
     * a value is fetched from the cache, it is verified by the given <tt>verifier</tt> in certain intervals before it
     * is returned to the user.
     * <p>
     * The system config can provide the following values:
     * <ul>
     * <li><tt>maxSize</tt>: max number of entries in the cache</li>
     * <li><tt>ttl</tt>: a duration specifying the max lifetime of a cached entry.</li>
     * <li><tt>verification</tt>: a duration specifying in which interval a verification of a value will
     * take place (if possible)</li>
     * </ul>
     * <p>
     * To create a cache which is maintained across a cluster of nodes, use
     * {@link #createCoherentCache(String, ValueComputer, ValueVerifier)}.
     *
     * @param name          the name of the cache, used to load the appropriate extension from the config
     * @param valueComputer used to compute a value, if no valid value was found in the cache for the given key. Can
     *                      be <tt>null</tt> if there is no appropriate way to compute such a value. In this case, the
     *                      cache will simply return <tt>null</tt>.
     * @param verifier      used to verify a value before it is returned to the user. Note that the
     *                      value is not verified each time, but in given intervals. If the verifier is <tt>null</tt>,
     *                      no verification will take place.
     * @param <K>           the key field used to identify cache entries
     * @param <V>           the value type used by the cache
     * @return a newly created cache according to the given parameters and the settings in the system config
     */
    public static <K, V> Cache<K, V> createLocalCache(String name,
                                                      ValueComputer<K, V> valueComputer,
                                                      ValueVerifier<V> verifier) {
        ManagedCache<K, V> result = new ManagedCache<>(name, valueComputer, verifier);

        verifyUniquenessOfName(name);
        caches.put(name, result);
        return result;
    }

    private static void verifyUniquenessOfName(String name) {
        if (caches.containsKey(name)) {
            throw Exceptions.handle()
                            .to(LOG)
                            .withSystemErrorMessage("A cache named '%s' has already been created!", name)
                            .handle();
        }
    }

    /**
     * Creates a cache with the given name, for which removals are roamed in the cluster to prevent stale values.
     * <p>
     * All other settings are exactly the same as for local caches. Also, if no {@link CacheCoherence} is present,
     * this will behave like a local cache.
     *
     * @param name          the name of the cache, used to load the appropriate extension from the config
     * @param valueComputer used to compute a value, if no valid value was found in the cache for the given key. Can
     *                      be <tt>null</tt> if there is no appropriate way to compute such a value. In this case, the
     *                      cache will simply return <tt>null</tt>.
     * @param verifier      used to verify a value before it is returned to the user. Note that the
     *                      value is not verified each time, but in given intervals. If the verifier is <tt>null</tt>,
     *                      no verification will take place.
     * @param <V>           the value type used by the cache
     * @return a newly created cache according to the given parameters and the settings in the system config
     * @see #createLocalCache(String, ValueComputer, ValueVerifier)
     */
    public static <V> Cache<String, V> createCoherentCache(String name,
                                                           ValueComputer<String, V> valueComputer,
                                                           ValueVerifier<V> verifier) {
        CoherentCache<V> result = new CoherentCache<>(name, valueComputer, verifier);

        verifyUniquenessOfName(name);
        caches.put(name, result);
        return result;
    }

    /**
     * Creates a locally managed cache with the given name.
     * <p>
     * This is just a shortcut for {@link #createLocalCache(String, ValueComputer, ValueVerifier)} with neither a
     * <tt>ValueComputer</tt> nor a <tt>ValueVerifier</tt> supplied.
     *
     * @param <K>  the key field used to identify cache entries
     * @param <V>  the value type used by the cache
     * @param name the name of the cache (used to fetch settings from the system config
     * @return the newly created cache
     * @see #createLocalCache(String, ValueComputer, ValueVerifier)
     */
    public static <K, V> Cache<K, V> createLocalCache(String name) {
        return createLocalCache(name, null, null);
    }

    /**
     * Creates a coherent cache with the given name.
     * <p>
     * This is just a shortcut for {@link #createCoherentCache(String, ValueComputer, ValueVerifier)} with neither a
     * <tt>ValueComputer</tt> nor a <tt>ValueVerifier</tt> supplied.
     *
     * @param <V>  the value type used by the cache
     * @param name the name of the cache (used to fetch settings from the system config
     * @return the newly created cache
     * @see #createLocalCache(String, ValueComputer, ValueVerifier)
     */
    public static <V> Cache<String, V> createCoherentCache(String name) {
        return createCoherentCache(name, null, null);
    }

    /**
     * Creates a new {@link InlineCache} with the given TTL and computer.
     * <p>
     * An inline cache can be used to compute a single value, which is then cached for a certain amount of time.
     *
     * @param ttl      specifies the duration which the computed value will be cached
     * @param computer the provider which is used to re-compute the value once it expired
     * @param <E>      the type of values being cached
     * @return an inline cache which keeps a computed value for the given amount of time and then uses the provided
     * computer to re-compute the value
     */
    public static <E> InlineCache<E> createInlineCache(Duration ttl, Supplier<E> computer) {
        return new InlineCache<>(computer, ttl.toMillis());
    }

    /**
     * Boilerplate method for {@link #createInlineCache(Duration, Supplier)}
     * which keeps the computed value for up to 10 seconds.
     *
     * @param computer the provider which is used to re-compute the value once it expired
     * @param <E>      the type of values being cached
     * @return an inline cache which keeps a computed value for ten seconds and then uses the provided
     * computer to re-compute the value
     */
    public static <E> InlineCache<E> createTenSecondsInlineCache(Supplier<E> computer) {
        return createInlineCache(INLINE_CACHE_DEFAULT_TTL, computer);
    }

    /**
     * Used by {@link CoherentCache} to signal that this cache is to be cleared on all nodes.
     *
     * @param cache the cache to clear
     */
    protected static void clearCoherentCache(CoherentCache<?> cache) {
        if (cacheCoherence != null) {
            cacheCoherence.clear(cache);
        } else {
            cache.clearLocal();
        }
    }

    /**
     * Clears the coherent cache locally.
     *
     * @param cacheName the cache to clear
     */
    public static void clearCoherentCacheLocally(String cacheName) {
        ManagedCache<?, ?> cache = caches.get(cacheName);
        if (cache instanceof CoherentCache) {
            ((CoherentCache<?>) cache).clearLocal();
        }
    }

    /**
     * Used by {@link CoherentCache} to signal that the given key should be removed from this cache on all nodes.
     *
     * @param cache the cache to remove the value from
     * @param key   the key to remove
     */
    protected static void removeCoherentCacheKey(CoherentCache<?> cache, String key) {
        if (cacheCoherence != null) {
            cacheCoherence.removeKey(cache, key);
        } else {
            cache.removeLocal(key);
        }
    }

    /**
     * Removes the given key from the given cache locally.
     *
     * @param cacheName the name of the cache to remove the value from
     * @param key       the key to remove
     */
    public static void removeCoherentCacheKeyLocally(String cacheName, String key) {
        ManagedCache<?, ?> cache = caches.get(cacheName);
        if (cache instanceof CoherentCache) {
            ((CoherentCache<?>) cache).removeLocal(key);
        }
    }

    /**
     * Notifies the other nodes about the delete handler to invoke.
     * <p>
     * This will invoke the delete handler previously registered via {@link Cache#addRemover(String, BiPredicate)}
     * which will then remove all matching cache entries.
     *
     * @param cache         the cache to cleanup
     * @param discriminator the name of the delete handler
     * @param testInput     the input into the predicate to determine which entries to delete
     */
    public static void removeAll(CoherentCache<?> cache, String discriminator, String testInput) {
        if (cacheCoherence != null) {
            cacheCoherence.removeAll(cache, discriminator, testInput);
        } else {
            cache.removeAllLocal(discriminator, testInput);
        }
    }

    /**
     * Executes the delete handler locally.
     *
     * @param cacheName     the name of the cache to remove the entries from
     * @param discriminator the name of the delete handler to invoke
     * @param testInput     the input for the predicate to identify entries to delete
     */
    public static void coherentCacheRemoveAllLocally(String cacheName, String discriminator, String testInput) {
        ManagedCache<?, ?> cache = caches.get(cacheName);
        if (cache instanceof CoherentCache) {
            ((CoherentCache<?>) cache).removeAllLocal(discriminator, testInput);
        }
    }

    /**
     * Clears the caches when Sirius is shutting down
     */
    @Register
    public static class CacheManagerLifecycle implements Stoppable {

        @Override
        public void stopped() {
            caches.values().forEach(ManagedCache::clear);
            caches.clear();
        }
    }
}
