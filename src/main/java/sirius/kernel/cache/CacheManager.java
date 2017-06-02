/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.cache;

import sirius.kernel.commons.Strings;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
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

    /*
     * This class has only static members and is not intended to be instantiated
     */
    private CacheManager() {
    }

    /*
     * Logged used by the caching system
     */
    protected static final Log LOG = Log.get("cache");

    /*
     * Lists all known caches.
     */
    private static List<ManagedCache<?, ?>> caches = new CopyOnWriteArrayList<>();

    /**
     * Returns a list of all known caches
     *
     * @return a list of all caches created so far
     */
    public static List<ManagedCache<?, ?>> getCaches() {
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
    public static <K, V> Cache<K, V> createCache(String name,
                                                 ValueComputer<K, V> valueComputer,
                                                 ValueVerifier<V> verifier) {
        verifyUniquenessOfName(name);
        ManagedCache<K, V> result = new ManagedCache<>(name, valueComputer, verifier);
        caches.add(result);
        return result;
    }

    private static void verifyUniquenessOfName(String name) {
        for (ManagedCache<?, ?> other : caches) {
            if (Strings.areEqual(name, other.getName())) {
                throw Exceptions.handle()
                                .to(LOG)
                                .withSystemErrorMessage("A cache named '%s' has already been created!", name)
                                .handle();
            }
        }
    }

    /**
     * Creates a cached with the given name.
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
     * @param ttl      specifies the duration in seconds which the computed value will be cached
     * @param computer the provider which is used to re-compute the value once it expired
     * @param <E>      the type of values being cached
     * @return an inline cache which keeps a computed value for the given amount of time and then uses the provided
     * computer to re-compute the value
     */
    public static <E> InlineCache<E> createInlineCache(Duration ttl, Supplier<E> computer) {
        return new InlineCache<>(computer, ttl.toMillis());
    }

    private static final Duration INLINE_CACHE_DEFAULT_TTL = Duration.ofSeconds(10);

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
}
