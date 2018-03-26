/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.cache;

import sirius.kernel.cache.distributed.DistributedCacheFactory;
import sirius.kernel.cache.distributed.ValueParser;
import sirius.kernel.commons.Explain;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.GlobalContext;
import sirius.kernel.di.std.Part;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Provides access to all managed caches
 * <p>
 * Is responsible for creating new caches using {@link #createCache(String)}. Also, this class keeps track of all
 * known caches.
 * <p>
 * Additionally instances of {@link InlineCache} can be created, which can be used to compute a single value,
 * which is then cached for a given amount of time.
 */
public class CacheManager {

    @Part
    private static GlobalContext globalContext;

    /**
     * Logged used by the caching system
     */
    public static final Log LOG = Log.get("cache");

    /**
     * Lists all known caches.
     */
    private static List<Cache<?, ?>> caches = new CopyOnWriteArrayList<>();

    private static final Duration INLINE_CACHE_DEFAULT_TTL = Duration.ofSeconds(10);

    /**
     * This class has only static members and is not intended to be instantiated
     */
    private CacheManager() {
    }

    /**
     * Returns a list of all known caches
     *
     * @return a list of all caches created so far
     */
    public static List<Cache<?, ?>> getCaches() {
        return Collections.unmodifiableList(caches);
    }

    /**
     * Creates a cache with the given name.
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
    @SuppressWarnings("squid:S2250")
    @Explain("Caches are only created once, so there is no performance hotspot")
    public static <K, V> Cache<K, V> createCache(String name,
                                                 ValueComputer<K, V> valueComputer,
                                                 ValueVerifier<V> verifier) {
        verifyUniquenessOfName(name);
        ManagedCache<K, V> result = new ManagedCache<>(name, valueComputer, verifier);
        caches.add(result);
        return result;
    }

    private static void verifyUniquenessOfName(String name) {
        for (Cache<?, ?> other : caches) {
            if (Strings.areEqual(name, other.getName())) {
                throw Exceptions.handle()
                                .to(LOG)
                                .withSystemErrorMessage("A cache named '%s' has already been created!", name)
                                .handle();
            }
        }
    }

    /**
     * Creates a cache with the given name.
     * <p>
     * This is just a shortcut for {@link #createCache(String, ValueComputer, ValueVerifier)} with neither a
     * <tt>ValueComputer</tt> nor a <tt>ValueVerifier</tt> supplied.
     *
     * @param <K>  the key field used to identify cache entries
     * @param <V>  the value type used by the cache
     * @param name the name of the cache (used to fetch settings from the system config
     * @return the newly created cache
     * @see #createCache(String, ValueComputer, ValueVerifier)
     */
    public static <K, V> Cache<K, V> createCache(String name) {
        return createCache(name, null, null);
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
     * Creates a distributed cache with the given name.
     * <p>
     * If no {@link DistributedCacheFactory} is found and configured, a regular cache is created via
     * {@link #createCache(String, ValueComputer, ValueVerifier)}.
     * <p>
     * This is just a shortcut for {@link #createDistributedCache(String, ValueComputer, ValueVerifier, ValueParser)}
     * with neither a <tt>ValueComputer</tt> nor a <tt>ValueVerifier</tt> supplied.
     *
     * @param name        the name of the cache (used to fetch settings from the system config
     * @param valueParser responsible for parsing the value from and to JSON.
     * @return the newly created cache
     * @see #createCache(String, ValueComputer, ValueVerifier)
     * @see #createDistributedCache(String, ValueComputer, ValueVerifier, ValueParser)
     */
    public static <V> Cache<String, V> createDistributedCache(String name, ValueParser<V> valueParser) {
        return createDistributedCache(name, null, null, valueParser);
    }

    /**
     * Creates a distributed cache with the given name.
     * <p>
     * If no {@link DistributedCacheFactory} is found and configured, a regular cache is created via
     * {@link #createCache(String, ValueComputer, ValueVerifier)}.
     * <p>
     * A <tt>valueParser</tt> has to be supplied, which is responisible for parsing th cached value from an to json.
     * This is necessary, because the value has to be serialized to somewhere, to be accessible for other nodes.
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
     *
     * @param name          the name of the cache, used to load the appropriate extension from the config
     * @param valueComputer used to compute a value, if no valid value was found in the cache for the given key. Can
     *                      be <tt>null</tt> if there is no appropriate way to compute such a value. In this case, the
     *                      cache will simply return <tt>null</tt>.
     * @param verifier      used to verify a value before it is returned to the user. Note that the
     *                      value is not verified each time, but in given intervals. If the verifier is <tt>null</tt>,
     *                      no verification will take place.
     * @param valueParser   responsible for parsing the value from and to JSON.
     * @param <V>           the type of the cached value.
     * @return a newly created cache according to the given parameters and the settings in the system config
     */
    @SuppressWarnings("squid:S2250")
    @Explain("Caches are only created once, so there is no performance hotspot")
    public static <V> Cache<String, V> createDistributedCache(String name,
                                                              ValueComputer<String, V> valueComputer,
                                                              ValueVerifier<V> verifier,
                                                              ValueParser<V> valueParser) {
        DistributedCacheFactory distributedCacheFactory = globalContext.getPart(DistributedCacheFactory.class);
        if (distributedCacheFactory == null || !distributedCacheFactory.isConfigured()) {
            LOG.WARN("No DistributedCacheFactory is found or ready (yet)! Creating regular cache. "
                     + "DistributedCacheFactories are injected at runtime, so maybe you need to wait for system start.");
            return createCache(name, valueComputer, verifier);
        }
        verifyUniquenessOfName(name);
        Cache<String, V> result =
                distributedCacheFactory.createDistributedCache(name, valueComputer, verifier, valueParser);
        caches.add(result);
        return result;
    }
}
