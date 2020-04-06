package sirius.kernel.cache;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The CacheRemoveBuilder serves as fluent API in conjunction with a {@link Cache}.
 * <p>
 * A CacheRemoveBuilder is created via {@link Cache#addRemover(String)}. It allows to build a method reference
 * for a cache remover step-by-step. For details on the cache removal, see {@link Cache#addRemover(String, BiPredicate)}.
 * <p>
 * The type parameter T is the type of the object this {@link CacheRemoverBuilder} operates on. Initially,
 * it is a {@link CacheEntry}, but it can be changed using the {@link #map} function.
 *
 * <h3>Usage example</h3>
 * The cache remover is an intuitive, stream-like API that maps the cache entries, and marks them for removal if
 * the fulfill the given conditions:
 * <pre>{@code
 * CacheManager.<Tuple<String, Object>>createCoherentCache("my-cache")
 *             .addRemover("my-remover")
 *             .map(CacheEntry::getValue)
 *             .alwaysRemove(entry -> entry.getFirst() == null)
 *             .map(Tuple::getSecond)
 *             .filter(Objects::nonNull)
 *             .removeIf(Strings::equals)
 * }</pre>
 * This is equivalent to
 * <pre>{@code
 * CacheManager.<Tuple<String, Object>>createCoherentCache("my-cache")
 *             .addRemover("my-remover", (test, entry) -> {
 *                 Tuple<String, Object> tuple = entry.getValue();
 *                 if (tuple.getFirst() == null) {
 *                     return false;
 *                 }
 *                 Object object = tuple.getSecond();
 *                 return object != null && Strings.equals(object, test);
 *             });
 * }</pre>
 * All methods are described in detail in their respective documentation.
 *
 * @param <K> the key type of the associated cache
 * @param <V> the value type of the associated cache
 * @param <T> the intermediate type that the builder works on
 * @see Cache#addRemover(String)
 * @see Cache#addRemover(String, BiPredicate)
 */
public interface CacheRemoverBuilder<K, V, T> {
    /**
     * Similar to {@link java.util.stream.Stream#map Stream.map}, this method applies a mapping function to the
     * objects this {@link CacheRemoverBuilder} operates on.
     *
     * @param mapper The mapping function that is applied
     * @param <R>    The new underlying type that replaces T
     * @return a new {@link CacheRemoverBuilder} with the mapped underlying type
     */
    <R> CacheRemoverBuilder<K, V, R> map(BiFunction<String, T, R> mapper);

    /**
     * Convenience method for {@link #map(BiFunction)} ignoring the test parameter.
     *
     * @param mapper The mapping function that is applied
     * @param <R>    The new underlying type that replaces T
     * @return a new {@link CacheRemoverBuilder} with the mapped underlying type
     */
    <R> CacheRemoverBuilder<K, V, R> map(Function<T, R> mapper);

    /**
     * Similar to {@link java.util.stream.Stream#filter Stream.filter}, this method filters the objects this
     * {@link CacheRemoverBuilder} operates on.
     * <p>
     * All objects that do not match the predicate are ignored, and will not be removed from the cache. This
     * allows to discard those objects early.
     *
     * @param predicate The predicate to test the objects
     * @return A new {@link CacheRemoverBuilder} operating on the remaining objects
     */
    CacheRemoverBuilder<K, V, T> filter(BiPredicate<String, T> predicate);

    /**
     * Convenience method for {@link #filter(BiPredicate)} ignoring the test parameter.
     *
     * @param predicate The predicate to test the objects
     * @return A new {@link CacheRemoverBuilder} operating on the remaining objects
     */
    CacheRemoverBuilder<K, V, T> filter(Predicate<T> predicate);

    /**
     * Removes all cache entries matching the given predicate.
     * <p>
     * This method serves as a counterpart for {@link #filter}. The objects that match the predicate will be
     * removed from the cache without further processing, but contrary to {@link #removeIf}, it does not terminate
     * the builder.
     *
     * @param predicate The predicate to test the objects
     * @return A new {@link CacheRemoverBuilder} operating on the remaining objects
     */
    CacheRemoverBuilder<K, V, T> removeAlways(BiPredicate<String, T> predicate);

    /**
     * Convenience method for {@link #removeAlways(BiPredicate)} ignoring the test parameter.
     *
     * @param predicate The predicate to test the objects
     * @return A new {@link CacheRemoverBuilder} operating on the remaining objects
     */
    CacheRemoverBuilder<K, V, T> removeAlways(Predicate<T> predicate);

    /**
     * Removes all cache entries matching the given predicate and terminates the builder.
     * <p>
     * This method terminates the builder. All objects that are not yet ignored via {@link #filter} or removed via {@link #removeAlways} are
     * evaluated with the given predicate. If they match the predicate, they will be removed from the cache.
     *
     * @param predicate The predicate to test the objects
     * @return The cache this {@link CacheRemoverBuilder} operates on
     */
    Cache<K, V> removeIf(BiPredicate<String, T> predicate);

    /**
     * Convenience method for {@link #removeIf(BiPredicate)} ignoring the test parameter.
     *
     * @param predicate The predicate to test the objects
     * @return The cache this {@link CacheRemoverBuilder} operates on
     */
    Cache<K, V> removeIf(Predicate<T> predicate);
}
