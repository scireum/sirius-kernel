package sirius.kernel.cache;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public interface CacheRemoverBuilder<K, V, T> {
    <R> CacheRemoverBuilder<K, V, R> map(BiFunction<String, T, R> mapper);

    Cache<K, V> removeIf(BiPredicate<String, T> predicate);
    <R> CacheRemoverBuilder<K, V, R> map(Function<T, R> mapper);

    Cache<K, V> removeIf(Predicate<T> predicate);
}
