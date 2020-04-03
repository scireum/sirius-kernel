package sirius.kernel.cache;

import sirius.kernel.commons.Tuple;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;

public class ManagedCacheRemoverBuilder<K, V, T> implements CacheRemoverBuilder<K, V, T> {
    private final Cache<K, V> cache;
    private final String discriminator;

    /**
     * The combined mapping/filter function up to now.
     * The {@link State} defines whether the object was already marked for removal via {@link #removeAlways} or
     * marked as filtered via {@link #filter}. If it is marked as undecided, it should be processed further.
     */
    private final BiFunction<String, CacheEntry<K, V>, Tuple<T, State>> mapper;

    private enum State {
        FILTERED, REMOVED, UNDECIDED
    }

    protected static <K, V> ManagedCacheRemoverBuilder<K, V, CacheEntry<K, V>> create(Cache<K, V> cache,
                                                                                      String discriminator) {
        return new ManagedCacheRemoverBuilder<>(cache,
                                                discriminator,
                                                (ignored, value) -> Tuple.create(value, State.UNDECIDED));
    }

    private ManagedCacheRemoverBuilder(Cache<K, V> cache,
                                       String discriminator,
                                       BiFunction<String, CacheEntry<K, V>, Tuple<T, State>> mapper) {
        this.cache = cache;
        this.discriminator = discriminator;
        this.mapper = mapper;
    }

    @Override
    public <R> CacheRemoverBuilder<K, V, R> map(BiFunction<String, T, R> mapper) {
        return new ManagedCacheRemoverBuilder<>(cache, discriminator, (selector, entry) -> {
            Tuple<T, State> t = this.mapper.apply(selector, entry);
            if (t.getSecond() != State.UNDECIDED) {
                return Tuple.create(null, t.getSecond());
            }
            return Tuple.create(mapper.apply(selector, t.getFirst()), State.UNDECIDED);
        });
    }

    @Override
    public CacheRemoverBuilder<K, V, T> filter(BiPredicate<String, T> predicate) {
        return new ManagedCacheRemoverBuilder<>(cache, discriminator, (selector, entry) -> {
            Tuple<T, State> t = this.mapper.apply(selector, entry);
            if (t.getSecond() == State.UNDECIDED && !predicate.test(selector, t.getFirst())) {
                return Tuple.create(null, State.FILTERED);
            }
            return t;
        });
    }

    @Override
    public CacheRemoverBuilder<K, V, T> removeAlways(BiPredicate<String, T> predicate) {
        return new ManagedCacheRemoverBuilder<>(cache, discriminator, (selector, entry) -> {
            Tuple<T, State> t = this.mapper.apply(selector, entry);
            if (t.getSecond() == State.UNDECIDED && predicate.test(selector, t.getFirst())) {
                return Tuple.create(null, State.REMOVED);
            }
            return t;
        });
    }

    @Override
    public Cache<K, V> removeIf(BiPredicate<String, T> predicate) {
        return cache.addRemover(discriminator, (selector, entry) -> {
            Tuple<T, State> t = mapper.apply(selector, entry);
            switch (t.getSecond()) {
                case FILTERED:
                    return false;
                case REMOVED:
                    return true;
                case UNDECIDED:
                default:
                    return predicate.test(selector, t.getFirst());
            }
        });
    }
}
