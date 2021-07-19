/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import java.util.function.Supplier;

/**
 * Uses the given supplier but caches the computed value after the first call to {@link #get()}.
 * <p>
 * This can be used to delay computation intensive operations up until they are needed and
 * also prevent from re-running a computation if the result is required several times.
 *
 * @param <T> the type of objects being supplied
 */
public class CachingSupplier<T> implements Supplier<T> {

    private ValueHolder<T> value;
    private final Supplier<T> supplier;

    /**
     * Creates a new instance with the operation to delay.
     *
     * @param supplier the supplier to invoke when necessarry
     */
    public CachingSupplier(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    /**
     * Invokes the given supplier and returns the cached result for every other call.
     *
     * @return the cached computation result of the given supplier.
     */
    @Override
    public T get() {
        if (value == null) {
            value = new ValueHolder<>(supplier.get());
        }

        return value.get();
    }
}
