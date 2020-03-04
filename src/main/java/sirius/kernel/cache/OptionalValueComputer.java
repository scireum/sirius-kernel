/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.cache;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Function;

/**
 * Implementation of {@link ValueComputer} that unwraps a function returning an {@link Optional}.
 * <p>
 * Can be supplied to {@link CacheManager#createLocalCache(String, ValueComputer, ValueVerifier)} or
 * {@link CacheManager#createCoherentCache(String, ValueComputer, ValueVerifier)} when creating a cache to compute
 * optional values which are not found in the cache, without storing the actual optional objects in the cache.
 *
 * @param <K> the key type determining the type of the lookup values in the cache
 * @param <V> the value type determining the type of values stored in the cache
 */
public class OptionalValueComputer<K, V> implements ValueComputer<K, V> {

    private Function<K, Optional<V>> computer;

    private OptionalValueComputer(Function<K, Optional<V>> computer) {
        this.computer = computer;
    }

    /**
     * Creates a new wrapper for value computer returning {@link Optional optionals}.
     *
     * @param computer the function to wrap
     * @param <K>      the key type determining the type of the lookup values in the cache
     * @param <V>      the value type determining the type of values stored in the cache
     * @return a new instance of {@link OptionalValueComputer}, wrapping the given function
     */
    public static <K, V> OptionalValueComputer<K, V> of(Function<K, Optional<V>> computer) {
        return new OptionalValueComputer<>(computer);
    }

    @Nullable
    @Override
    public V compute(@Nonnull K key) {
        return computer.apply(key).orElse(null);
    }
}
