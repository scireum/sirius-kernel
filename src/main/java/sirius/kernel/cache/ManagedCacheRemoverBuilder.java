package sirius.kernel.cache;

import sirius.kernel.commons.Tuple;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

class ManagedCacheRemoverBuilder<K, V, T> implements CacheRemoverBuilder<K, V, T> {

    private enum State {
        FILTERED, REMOVED, UNDECIDED
    }

    /**
     * The combined mapping/filter function up to now.
     * <p>
     * The {@link State} defines whether the object was already marked for removal via {@link #removeAlways} or
     * marked as filtered via {@link #filter}. If it is marked as undecided, it should be processed further.
     */
    private final BiFunction<String, CacheEntry<K, V>, Tuple<T, State>> mapper;
    private final Cache<K, V> cache;
    private final String discriminator;


    private ManagedCacheRemoverBuilder(Cache<K, V> cache,
                                       String discriminator,
                                       BiFunction<String, CacheEntry<K, V>, Tuple<T, State>> mapper) {
        this.cache = cache;
        this.discriminator = discriminator;
        this.mapper = mapper;
    }

    protected static <K, V> ManagedCacheRemoverBuilder<K, V, CacheEntry<K, V>> create(Cache<K, V> cache,
                                                                                      String discriminator) {
        return new ManagedCacheRemoverBuilder<>(cache,
                                                discriminator,
                                                (ignored, value) -> Tuple.create(value, State.UNDECIDED));
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
            return switch (t.getSecond()) {
                case FILTERED -> false;
                case REMOVED -> true;
                default -> predicate.test(selector, t.getFirst());
            };
        });
    }

    @Override
    public <R> CacheRemoverBuilder<K, V, R> map(Function<T, R> mapper) {
        return map((string, value) -> mapper.apply(value));
    }

    @Override
    public CacheRemoverBuilder<K, V, T> filter(Predicate<T> predicate) {
        return filter((string, value) -> predicate.test(value));
    }

    @Override
    public CacheRemoverBuilder<K, V, T> removeAlways(Predicate<T> predicate) {
        return removeAlways((string, value) -> predicate.test(value));
    }

    @Override
    public Cache<K, V> removeIf(Predicate<T> predicate) {
        return removeIf((string, value) -> predicate.test(value));
    }
}
