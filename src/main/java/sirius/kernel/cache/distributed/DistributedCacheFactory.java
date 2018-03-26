/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.cache.distributed;

import sirius.kernel.cache.Cache;
import sirius.kernel.cache.ValueComputer;
import sirius.kernel.cache.ValueVerifier;

/**
 * Responsible for creating a distributed cache.
 */
public interface DistributedCacheFactory {

    /**
     * Creates a distributed cache with the given name.
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
    public <V> Cache<String, V> createDistributedCache(String name,
                                                       ValueComputer<String, V> valueComputer,
                                                       ValueVerifier<V> verifier,
                                                       ValueParser<V> valueParser);

    /**
     * Returns whether the DistributedCacheFactory is configured and can create caches.
     */
    boolean isConfigured();
}
