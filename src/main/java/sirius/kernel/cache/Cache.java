/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.cache;

import sirius.kernel.commons.Callback;
import sirius.kernel.commons.Tuple;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Provides a cache which can be used to store and access values.
 * <p>
 * In contrast to a simple Map, the cache has a maximum size. If the cache is full, old (least recently used)
 * entries are evicted to make room for newer ones. Also this cache will be automatically evicted from time to
 * time so that only values which are really used remain in the cache. Therefore system resources (most essentially
 * heap storage) is released if no longer required.
 * <p>
 * A new Cache is created by invoking {@link CacheManager#createLocalCache(String)}. The maximal size as well as the
 * time to live value for each entry is set via the <tt>cache.[cacheName]</tt> extension. Additionally
 * {@link CacheManager#createLocalCache(String, ValueComputer, ValueVerifier)} can be used to supply a
 * <tt>ValueComputer</tt> as well as a <tt>ValueVerifier</tt>. Those classes are responsible for creating non-
 * existent cache values or to verify that cached values are still up to date, before they are returned to a
 * user of the cache.
 *
 * @param <K> the key type determining the type of the lookup values in the cache
 * @param <V> the value type determining the type of values stored in the cache
 * @see CacheManager
 * @see ValueComputer
 * @see ValueVerifier
 */
public interface Cache<K, V> {

    /**
     * Returns the name of the cache, which is also used to load the configuration.
     *
     * @return the name of the cache
     */
    String getName();

    /**
     * Returns the max size of the cache
     *
     * @return the maximal number of cached entries
     */
    int getMaxSize();

    /**
     * Returns the number of entries in the cache
     *
     * @return the number of cached entries
     */
    int getSize();

    /**
     * Returns the number of reads since the last eviction
     *
     * @return the number of uses of this cache since the last eviction run
     */
    long getUses();

    /**
     * Returns the statistical values of "uses" for the last some eviction
     * intervals.
     *
     * @return a list of uses for the last N eviction intervals
     */
    List<Long> getUseHistory();

    /**
     * Returns the cache hit-rate (in percent)
     *
     * @return the cache hit-rate (successful gets) of this cache since the last eviction
     */
    Long getHitRate();

    /**
     * Returns the statistical values of "hit rate" for the last some eviction
     * intervals.
     *
     * @return a list of hit-rates for the last N eviction intervals
     */
    List<Long> getHitRateHistory();

    /**
     * Returns the date of the last eviction run.
     *
     * @return the timestamp of the last eviction
     */
    Date getLastEvictionRun();

    /**
     * Clears the complete cache
     */
    void clear();

    /**
     * Returns the value associated with the given key.
     *
     * @param key the key used to retrieve the value in the cache
     * @return the cached value or <tt>null</tt> if neither a valid value was found, nor one could be
     * computed.
     */
    @Nullable
    V get(@Nonnull K key);

    /**
     * Returns the value associated with the given key. If the value is not
     * found, the {@link ValueComputer} is invoked.
     *
     * @param key      the key used to retrieve the value in the cache
     * @param computer the computer used to generate a value if absent in the cache
     * @return the cached value or <tt>null</tt> if neither a valid value was found, nor one could be
     * computed.
     */
    @Nullable
    V get(@Nonnull K key, @Nullable ValueComputer<K, V> computer);

    /**
     * Stores the given key value mapping in the cache
     *
     * @param key   the key used to store this entry
     * @param value the value to be stored in the entry
     */
    void put(@Nonnull K key, @Nullable V value);

    /**
     * Removes the given item from the cache
     *
     * @param key the key which should be removed from the cache
     */
    void remove(@Nonnull K key);

    /**
     * Adds a remove handler identified with the given <tt>disciminator</tt>.
     * <p>
     * Such handlers can be invoked via {@link #removeAll(String, String)} and use the given <tt>testInput</tt>
     * to filter an remove all matching entries from the cache. This can be used to remove a bunch of entries at
     * once, which can all be identified via a single property.
     * <p>
     * Being all string based, this also works for coherent caches by simply broadcasting two short string values.
     *
     * @param disciminator the name of the remover
     * @param test         the predicate which determines if a given entry matches a given test input
     * @return the cache itself for fluent method calls
     */
    Cache<K, V> addRemover(@Nonnull String disciminator, @Nonnull BiPredicate<String, CacheEntry<K, V>> test);

    /**
     * Invokes the given remover with the given test input.
     * <p>
     * Invokes a remover which has previously been registered via {@link #addRemover(String, BiPredicate)} and
     * removes all entries for which the predicate returns <tt>true</tt> for the given <tt>testInput</tt>.
     *
     * @param discriminator the remover to invoke
     * @param testInput     the input to pass into the predicate
     */
    void removeAll(@Nonnull String discriminator, String testInput);

    /**
     * Removes all cached values for which the predicate returns true.
     *
     * @param predicate the predicate used to determine if a value should be removed from the cache.
     * @deprecated Because in coherenct cache environments this can lead to stale cache entries if a cache on
     * one nodes has a different set of keys than another, as the scan always runs locally.
     * Use {@link #addRemover(String, BiPredicate)} and {@link #removeAll(String, String)} which scans each node
     * individually.
     */
    @Deprecated
    void removeIf(@Nonnull Predicate<CacheEntry<K, V>> predicate);

    /**
     * Provides access to the keys stored in this cache
     *
     * @return an Iterator for all keys in the cache
     */
    Iterator<K> keySet();

    /**
     * Provides access to the contents of this cache
     *
     * @return a list of entries which provide detailed information about each entry in the cache
     */
    List<CacheEntry<K, V>> getContents();

    /**
     * Sets the remove callback which is invoked once a value is removed from the cache.
     * <p>
     * Only one handler can be set at a time.
     *
     * @param onRemoveCallback the callback to call when an element is removed from the cache. Can be null
     *                         <tt>null</tt> to remove the last handler.
     * @return the original instance of the cache.
     */
    Cache<K, V> onRemove(Callback<Tuple<K, V>> onRemoveCallback);
}
