/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import com.google.common.base.Objects;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * Represents a tuple of two values with two arbitrary types.
 * <p>
 * If the first type is comparable and should be used to compare the tuples, {@link ComparableTuple} can be used.
 *
 * @param <F> defines the first type of the tuple
 * @param <S> defines the second type of the tuple
 * @author Andreas Haufler (aha@scireum.de)
 * @see Tuple
 * @see Comparable
 * @since 2013/08
 */
public class Tuple<F, S> {

    private F first;
    private S second;

    /**
     * Creates a new tuple with both values set to <tt>null</tt>
     *
     * @param <F> defines the first type of the tuple
     * @param <S> defines the second type of the tuple
     * @return the newly created tuple
     */
    public static <F, S> Tuple<F, S> create() {
        return new Tuple<>(null, null);
    }

    /**
     * Creates a tuple with a given value for <tt>first</tt>
     *
     * @param first defines the value to be used for the first component of the tuple
     * @param <F>   defines the first type of the tuple
     * @param <S>   defines the second type of the tuple
     * @return the newly created tuple
     */
    public static <F, S> Tuple<F, S> create(F first) {
        return new Tuple<>(first, null);
    }

    /**
     * Creates a tuple with a givens value for <tt>first</tt> and <tt>second</tt>
     *
     * @param first  defines the value to be used for the first component of the tuple
     * @param second defines the value to be used for the second component of the tuple
     * @param <F>    defines the first type of the tuple
     * @param <S>    defines the second type of the tuple
     * @return the newly created tuple
     */
    public static <F, S> Tuple<F, S> create(F first, S second) {
        return new Tuple<F, S>(first, second);
    }

    /**
     * Creates a tuple with a givens value for <tt>first</tt> and <tt>second</tt>
     * <p>
     * Can be used to specify the generic types for F and S. Otherwise, the <tt>create</tt> methods can be used.
     *
     * @param first  defines the value to be used for the first component of the tuple
     * @param second defines the value to be used for the second component of the tuple
     */
    public Tuple(F first, S second) {
        super();
        this.first = first;
        this.second = second;
    }

    /**
     * Returns the <tt>first</tt> component of the tuple
     *
     * @return the first component of the tuple
     */
    public F getFirst() {
        return first;
    }

    /**
     * Sets the <tt>first</tt> component of the tuple to the given value.
     *
     * @param first defines the value to be used as the first component of the tuple
     */
    public void setFirst(F first) {
        this.first = first;
    }

    /**
     * Returns the <tt>second</tt> component of the tuple
     *
     * @return the second component of the tuple
     */
    public S getSecond() {
        return second;
    }

    /**
     * Sets the <tt>second</tt> component of the tuple to the given value.
     *
     * @param second defines the value to be used as the second component of the tuple
     */
    public void setSecond(S second) {
        this.second = second;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Tuple<?, ?>)) {
            return false;
        }
        Tuple<?, ?> other = (Tuple<?, ?>) obj;

        return Objects.equal(first, other.getFirst()) && Objects.equal(second, other.getSecond());
    }

    @Override
    public String toString() {
        return first + ": " + second;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(first, second);
    }

    /**
     * Extracts all <tt>first</tt> components of the given collection of tuples and returns them as list.
     *
     * @param tuples the collection of tuples to process
     * @param <T>    the type of the tuples involved
     * @param <K>    the type of the first elements of the tuples
     * @param <V>    the type of the second elements of the tuples
     * @return a list containing each <tt>first</tt> component of the collection of given tuples.
     */
    public static <T extends Tuple<K, V>, K, V> List<K> firsts(@Nonnull Collection<T> tuples) {
        List<K> result = new ArrayList<>(tuples.size());
        for (Tuple<K, V> t : tuples) {
            result.add(t.getFirst());
        }
        return result;
    }

    /**
     * Extracts all <tt>second</tt> components of the given collection of tuples and returns them as list.
     *
     * @param tuples the collection of tuples to process
     * @param <T>    the type of the tuples involved
     * @param <K>    the type of the first elements of the tuples
     * @param <V>    the type of the second elements of the tuples
     * @return a list containing each <tt>second</tt> component of the collection of given tuples.
     */
    public static <T extends Tuple<K, V>, K, V> List<V> seconds(@Nonnull Collection<T> tuples) {
        List<V> result = new ArrayList<>(tuples.size());
        for (Tuple<K, V> t : tuples) {
            result.add(t.getSecond());
        }
        return result;
    }

    /**
     * Converts a map into a list of tuples.
     *
     * @param map the map to be converted
     * @param <K> the key type of the map and therefore the type of the first component of the tuples
     * @param <V> the value type of the map and therefore the type of the second component of the tuples
     * @return a list of tuples, containing one tuple per map entry where the first component is the key,
     * and the second component is the value of the map entry.
     */
    public static <K, V> List<Tuple<K, V>> fromMap(@Nonnull Map<K, V> map) {
        List<Tuple<K, V>> result = new ArrayList<>(map.size());
        for (Map.Entry<K, V> e : map.entrySet()) {
            result.add(new Tuple<>(e.getKey(), e.getValue()));
        }
        return result;
    }

    /**
     * Converts a collection of tuples into a map
     *
     * @param values the collection of tuples to be converted
     * @param <K>    the key type of the map and therefore the type of the first component of the tuples
     * @param <V>    the value type of the map and therefore the type of the second component of the tuples
     * @return a map containing an entry for each tuple in the collection, where the key is the first component of the
     * tuple and the value is the second component of the tuple. If two tuples have equal values as first
     * component, the specific map entry will be overridden in the order defined in the given collection.
     */
    public static <K, V> Map<K, V> toMap(@Nonnull Collection<Tuple<K, V>> values) {
        Map<K, V> result = new HashMap<>();
        for (Tuple<K, V> e : values) {
            result.put(e.getFirst(), e.getSecond());
        }
        return result;
    }

    /**
     * Provides a {@link Collector} which can be used to collect a {@link Stream} of tuples into a {@link Map}.
     * <p>
     * As an example: <code>aStream.collect(Tuple.toMap(HashMap::new, (a, b) -&gt; b))</code> will transform the
     * stream of tuples into a map where a later key value pair will overwrite earlier ones.
     *
     * @param supplier factory for generating the result map
     * @param merger   used to decide which value to keep on a key collision
     * @param <K>      key type of the tuples being processed
     * @param <V>      value type of the tuples being processed
     * @return a <tt>Collector</tt> which transforms a stream of tuples into a map
     * @see #toMap(java.util.function.Supplier)
     */
    public static <K, V> Collector<Tuple<K, V>, Map<K, V>, Map<K, V>> toMap(Supplier<Map<K, V>> supplier,
                                                                            BinaryOperator<V> merger) {
        return Collector.of(supplier, (map, tuple) -> map.put(tuple.getFirst(), tuple.getSecond()), (a, b) -> {
                    b.entrySet()
                            .forEach(entryInB -> a.compute(entryInB.getKey(),
                                    (key, valueOfA) -> merger.apply(valueOfA,
                                            entryInB.getValue())
                            ));
                    return a;
                }, Function.identity(), Collector.Characteristics.IDENTITY_FINISH
        );
    }

    /**
     * Provides a {@link Collector} which can be used to collect a {@link Stream} of tuples into a {@link Map}.
     * <p>
     * Key collisions are automatically handled by choosing the later entry (updating the map).
     *
     * @param supplier factory for generating the result map
     * @param <K>      key type of the tuples being processed
     * @param <V>      value type of the tuples being processed
     * @return a <tt>Collector</tt> which transforms a stream of tuples into a map
     */
    public static <K, V> Collector<Tuple<K, V>, Map<K, V>, Map<K, V>> toMap(Supplier<Map<K, V>> supplier) {
        return toMap(supplier, (a, b) -> b);
    }

    /**
     * Provides a {@link Collector} which can be used to collect a {@link Stream} of tuples into a {@link MultiMap}.
     * <p>
     * The type of <tt>MultiMap</tt> used can be determined by the <tt>supplier</tt>. So for example
     * <code>MultiMap::createOrdered</code> will create a map with ordered keys.
     *
     * @param supplier factory for generating the result map
     * @param <K>      key type of the tuples being processed
     * @param <V>      value type of the tuples being processed
     * @return a <tt>Collector</tt> which transforms a stream of tuples into a multi map
     */
    public static <K, V> Collector<Tuple<K, V>, MultiMap<K, V>, MultiMap<K, V>> toMultiMap(Supplier<MultiMap<K, V>> supplier) {
        return Collector.of(supplier,
                (map, tuple) -> map.put(tuple.getFirst(), tuple.getSecond()),
                (a, b) -> a.merge(b),
                Function.identity(),
                Collector.Characteristics.IDENTITY_FINISH);
    }

    /**
     * Maps an entry which contains a collection as value into a {@link java.util.stream.Stream} of tuples, containing
     * the key of the entry along with a value of the collection.
     * <p>
     * This method is designed to be used with {@link Stream#flatMap(java.util.function.Function)}:
     * <pre>
     * <code>
     *      map.entrySet().flatMap(e -&gt; Tuple.flatten(e)).forEach(t -&gt; System.out.println(t));
     * </code>
     * </pre>
     *
     * @param entry the entry to transform
     * @param <K>   the key type of the entry
     * @param <V>   the value type of the entry
     * @return a <tt>Stream</tt> of tuples representing the original entry
     */
    public static <K, V> Stream<Tuple<K, V>> flatten(Map.Entry<K, Collection<V>> entry) {
        return entry.getValue().stream().map(v -> Tuple.create(entry.getKey(), v));
    }

    /**
     * Converts a {@link java.util.Map.Entry} into a tuple.
     *
     * @param entry the entry to convert
     * @param <K>   the key type of the entry
     * @param <V>   the value type of the entry
     * @return a tuple containing the key as first and the value as second parameter
     */
    public static <K, V> Tuple<K, V> valueOf(Map.Entry<K, V> entry) {
        return Tuple.create(entry.getKey(), entry.getValue());
    }

}
