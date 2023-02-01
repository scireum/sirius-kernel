/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * Represents a map which contains a collection of elements per key.
 * <p>
 * Provides an implementation which simulates a {@code Map&lt;K, Collection&lt;V&gt;&gt;} by providing
 * specific <tt>put</tt>, <tt>get</tt> and <tt>remove</tt> methods.
 *
 * @param <K> the key type used by the map
 * @param <V> the value type used by the map
 */
public class MultiMap<K, V> {

    protected Map<K, Collection<V>> base;

    /**
     * Used the static factory methods <tt>create</tt> or <tt>createdSynchronized</tt> to obtain an instance.
     *
     * @param base the underlying map to use
     */
    protected MultiMap(Map<K, Collection<V>> base) {
        this.base = base;
    }

    /**
     * Creates a new <tt>MultiMap</tt> for the specified types which is not thread safe.
     *
     * @param <K> the type of the keys used in the map
     * @param <V> the type of the values used withing the value lists of the map
     * @return a new instance of <tt>MultiMap</tt> which is not thread safe.
     */
    public static <K, V> MultiMap<K, V> create() {
        return new MultiMap<>(new HashMap<>());
    }

    /**
     * Creates a new <tt>MultiMap</tt> for the specified types which is not thread safe but keeps its insertion order.
     *
     * @param <K> the type of the keys used in the map
     * @param <V> the type of the values used withing the value lists of the map
     * @return a new instance of <tt>MultiMap</tt> which is not thread safe.
     */
    public static <K, V> MultiMap<K, V> createOrdered() {
        return new MultiMap<>(new LinkedHashMap<>());
    }

    /**
     * Creates a new <tt>MultiMap</tt> for the specified types which is thread safe.
     *
     * @param <K> the type of the keys used in the map
     * @param <V> the type of the values used withing the value lists of the map
     * @return a new instance of <tt>MultiMap</tt> which is thread safe.
     */
    public static <K, V> MultiMap<K, V> createSynchronized() {
        return new MultiMap<K, V>(Collections.synchronizedMap(new HashMap<>())) {
            @Override
            @SuppressWarnings("squid:S1185")
            @Explain("We need to overwrite this to make it synchronized.")
            public synchronized void put(K key, V value) {
                super.put(key, value);
            }

            @Override
            protected List<V> createValueList() {
                return new CopyOnWriteArrayList<>();
            }
        };
    }

    /**
     * Adds the given value to the list of values kept for the given key.
     * <p>
     * Note that the values for a given key don't from a <tt>Set</tt>. Therefore adding the same value twice
     * for the same key, will result in having a value list containing the added element twice.
     *
     * @param key   the key for which the value is added to the map
     * @param value the value which is added to the list of values for this key
     */
    public void put(@Nonnull K key, @Nullable V value) {
        Collection<V> list = base.computeIfAbsent(key, k -> createValueList());
        list.add(value);
    }

    /**
     * Sets the given value to the given name.
     * <p>
     * All previously set values will be removed.
     *
     * @param key   the key for which the value is added to the map
     * @param value the name (and only) value for the given key
     */
    public void set(@Nonnull K key, @Nullable V value) {
        Collection<V> list = base.get(key);
        if (list == null) {
            list = createValueList();
            base.put(key, list);
        } else {
            list.clear();
        }
        list.add(value);
    }

    /**
     * Can be overridden to specify the subclass of <tt>List</tt> used to store value lists.
     *
     * @return a new instance which is used as value list for a key.
     */
    protected List<V> createValueList() {
        return new ArrayList<>();
    }

    /**
     * Removes all occurrences of the given value in the value list of the given key.
     * <p>
     * If the value does not occur in the value list or if the key is completely unknown, nothing will happen.
     *
     * @param key   the key of which value list the value will be removed from
     * @param value the value which will be removed from the value list
     */
    public void remove(@Nonnull K key, @Nullable V value) {
        Collection<V> list = base.get(key);
        if (list != null) {
            while (list.remove(value)) {
                //iterate...
            }
        }
    }

    /**
     * Returns the value list for the given key.
     * <p>
     * If the key is completely unknown, an empty list will be returned.
     *
     * @param key the key which value list is to be returned
     * @return the value map associated with the given key or an empty list is the key is unknown
     */
    @Nonnull
    public Collection<V> get(@Nonnull K key) {
        Collection<V> list = base.get(key);
        if (list == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(list);
    }

    /**
     * Returns the set of known keys.
     *
     * @return returns the set of known keys, that is keys for which <tt>put</tt> was called
     */
    @Nonnull
    public Set<K> keySet() {
        return base.keySet();
    }

    /**
     * Returns the unmodifiable set of known keys.
     *
     * @return returns the set of known keys, that is keys for which <tt>put</tt> was called
     */
    @Nonnull
    public Set<K> unmodifiableKeySet() {
        return Collections.unmodifiableSet(keySet());
    }

    /**
     * Provides direct access to the underlying map.
     * <p>
     * For the sake of simplicity and extensibility, the original map is returned. Therefore manipulations should
     * be well considered.
     *
     * @return the underlying <tt>Map</tt> of this instance.
     */
    @Nonnull
    public Map<K, Collection<V>> getUnderlyingMap() {
        return base;
    }

    /**
     * Returns a list of all values for all keys.
     * <p>
     * Note that this list has no <tt>Set</tt> like behaviour. Therefore the same value might occur several times
     * if it was added more than once for the same or for different keys.
     *
     * @return a list of all values stored for all keys
     */
    @Nonnull
    public List<V> values() {
        List<V> result = new ArrayList<>();
        for (Collection<V> val : getUnderlyingMap().values()) {
            result.addAll(val);
        }
        return result;
    }

    /**
     * Removes all entries from this map
     */
    public void clear() {
        getUnderlyingMap().clear();
    }

    @Override
    public String toString() {
        if (base == null) {
            return "(empty)";
        }
        return base.toString();
    }

    /**
     * Merges the given multi map into this one.
     * <p>
     * If both maps contain values for the same key, to lists will be joined together.
     * <p>
     * <b>Note</b>: This will modify the callee instead of creating a new result map
     *
     * @param other the other map to merge into this one
     * @return the callee itself for further processing
     */
    public MultiMap<K, V> merge(MultiMap<K, V> other) {
        if (other != null) {
            other.base.entrySet().stream().flatMap(Tuple::flatten).forEach(t -> put(t.getFirst(), t.getSecond()));
        }
        return this;
    }

    /**
     * Creates a {@link Collector} which can be used to group a stream into a multi map.
     *
     * @param supplier   the factory for creating the result map
     * @param classifier the method used to extract the key from the elements
     * @param <K>        the extracted key type of the map
     * @param <V>        the value type of the incoming stream and outgoing map
     * @return a <tt>Collector</tt> to be used with {@link Stream#collect(java.util.stream.Collector)}
     */
    public static <K, V> Collector<V, MultiMap<K, V>, MultiMap<K, V>> groupingBy(Supplier<MultiMap<K, V>> supplier,
                                                                                 Function<V, K> classifier) {
        return Collector.of(supplier,
                            (map, value) -> map.put(classifier.apply(value), value),
                            (a, b) -> a.merge(b),
                            Function.identity(),
                            Collector.Characteristics.IDENTITY_FINISH);
    }

    /**
     * Creates a {@link Collector} which can be used to group a stream into a multi map.
     * <p>
     * This method permits the classifier function to return multiple keys for a single element. The element will
     * be added for all returned keys.
     *
     * @param supplier   the factory for creating the result map
     * @param classifier the method used to extract the keys from the elements
     * @param <K>        the extracted key type of the map
     * @param <V>        the value type of the incoming stream and outgoing map
     * @return a <tt>Collector</tt> to be used with {@link Stream#collect(java.util.stream.Collector)}
     */
    public static <K, V> Collector<V, MultiMap<K, V>, MultiMap<K, V>> groupingByMultiple(Supplier<MultiMap<K, V>> supplier,
                                                                                         Function<V, Collection<K>> classifier) {
        return Collector.of(supplier,
                            (map, value) -> classifier.apply(value).forEach(key -> map.put(key, value)),
                            (a, b) -> a.merge(b),
                            Function.identity(),
                            Collector.Characteristics.IDENTITY_FINISH);
    }

    /**
     * Boilerplate method to access all entries of this map as {@link Stream}.
     * <p>
     * <b>Note</b>: Calling {@link sirius.kernel.commons.Tuple#flatten(java.util.Map.Entry)} via
     * {@link Stream#flatMap(java.util.function.Function)} will transform the resulting stream into a stream of
     * all pairs represented by this map.
     *
     * @return a <tt>Stream</tt> containing all entries of this map
     */
    public Stream<Map.Entry<K, Collection<V>>> stream() {
        return getUnderlyingMap().entrySet().stream();
    }
}
