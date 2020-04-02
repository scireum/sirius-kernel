package sirius.kernel.cache;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public class ManagedCacheRemoverBuilder<K, V, T> implements CacheRemoverBuilder<K, V, T> {
    private final Cache<K, V> cache;
    private final String discriminator;
    private final BiFunction<String, CacheEntry<K, V>, T> mapper;

    protected ManagedCacheRemoverBuilder(Cache<K, V> cache,
                                         String discriminator,
                                         BiFunction<String, CacheEntry<K, V>, T> mapper) {
        this.cache = cache;
        this.discriminator = discriminator;
        this.mapper = mapper;
    }

    @Override
    public <R> CacheRemoverBuilder<K, V, R> map(BiFunction<String, T, R> mapper) {
        return new ManagedCacheRemoverBuilder<>(cache, discriminator, (selector, entry) -> {
            T t = this.mapper.apply(selector, entry);
            return mapper.apply(selector, t);
        });
    }

    @Override
    public Cache<K, V> removeIf(BiPredicate<String, T> predicate) {
        return cache.addRemover(discriminator, (selector, entry) -> {
            T t = mapper.apply(selector, entry);
            return predicate.test(selector, t);
        });
    }

    @Override
    public <R> CacheRemoverBuilder<K, V, R> map(Function<T, R> mapper) {
        return map((ignored, t) -> mapper.apply(t));
    }

    @Override
    public Cache<K, V> removeIf(Predicate<T> predicate) {
        return removeIf((ignored, t) -> predicate.test(t));
    }
}
