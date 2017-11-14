/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;

/**
 * Helper class which provides various methods to work with lambdas.
 */
public class Lambdas {

    private Lambdas() {
    }

    /**
     * Provides an identity function which permits to "touch" the element for which it was called.
     * <p>
     * This is designed to be used with {@link java.util.Optional} like this:
     * {@code return Optional.of("Test").map(Lambdas.touch(s -&gt; System.out.println(s)))}
     *
     * @param consumer the lambda used to "touch" (use) each parameter
     * @param <T>      the type of the elements accepted by the consumer
     * @return an identity function which also applies the <tt>consumer</tt> on each parameter
     */
    public static <T> Function<T, T> touch(Consumer<T> consumer) {
        return t -> {
            consumer.accept(t);
            return t;
        };
    }

    /**
     * Used to collect the results of a stream operation into an existing collection.
     *
     * @param collection the target collection
     * @param <T>        the type of the elements accepted by the collector
     * @param <C>        the type of the collection which is filled by the collector
     * @return a {@link java.util.stream.Collector} inserting all elements into the given collection
     */
    public static <T, C extends Collection<T>> Collector<T, ?, C> into(C collection) {
        return Collector.of(() -> collection,
                            Collection::add,
                            Lambdas::unsupportedBiOperation,
                            Function.identity(),
                            Collector.Characteristics.IDENTITY_FINISH);
    }

    /**
     * Filters the given collection using the given predicate.
     * <p>
     * In contrast to the stream API of Java, this MODIFIES the collection which is passed in. This is certainly
     * no the "functional way" to do it - but Java not being a functional language there are some use cases to
     * modify an existing collection instead of creating a new one.
     *
     * @param collection to collection to remove elements from
     * @param filter     the filter which evaluates to <tt>true</tt> for each element to be removed
     * @param <T>        the type of the elements within the collection
     * @param <C>        the type of the collection which is processed
     * @return the filtered (modified) collection
     * @deprecated simply use {@link Collection#removeIf(Predicate)}.
     */
    @Deprecated
    public static <T, C extends Collection<T>> C remove(C collection, Predicate<T> filter) {
        collection.removeIf(filter);
        return collection;
    }

    /**
     * Can be used to group a given stream by identity and count the occurrences of each entity.
     *
     * @param <K> the type of the key by which the values are grouped
     * @return a Collector which can be supplied to {@link java.util.stream.Stream#collect(java.util.stream.Collector)}.
     */
    public static <K> Collector<K, Map<K, Integer>, Map<K, Integer>> groupAndCount() {
        return Collector.of(HashMap::new,
                            Lambdas::increment,
                            Lambdas::unsupportedBiOperation,
                            Function.identity(),
                            Collector.Characteristics.IDENTITY_FINISH);
    }

    private static <K> void increment(Map<K, Integer> map, K value) {
        map.put(value, map.computeIfAbsent(value, k -> 0) + 1);
    }

    /**
     * Can be used as lambda for unsupported BiOperations.
     * <p>
     * This is intended to be a dummy parameter (e.g. for <tt>Collector.of</tt>. It will always throw
     * an <tt>UnsupportedOperationException</tt> if invoked.
     *
     * @param <O> the type of objects for which the operation is to be used
     * @param a   the first parameter of the binary operation
     * @param b   the second parameter of the binary operation
     * @return this method will never return a value as an <tt>UnsupportedOperationException</tt> is thrown
     * @throws java.lang.UnsupportedOperationException always thrown by this method
     */
    public static <O> O unsupportedBiOperation(O a, O b) {
        throw new UnsupportedOperationException();
    }
}
