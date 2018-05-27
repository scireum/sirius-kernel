/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

/**
 * Provides a value for the given key just like a {@link java.util.function.Function}.
 * <p>
 * However, the return value is wrapped as {@link Value}. This permits to distinguish between a <tt>null</tt>
 * and <b>no value</b>. Also by declaring <tt>Exception</tt> in the throws clause, we delegate the exception handling
 * from the callee to the caller.
 */
public interface ValueSupplier<K> {

    /**
     * Computes or fetches the value for the given key.
     *
     * @param key the key to fetch the value for
     * @return the value for the given key or <tt>null</tt> to indicate that no value is available.
     * @throws Exception in case of an error while fetching the value.
     */
    Value apply(K key) throws Exception;
}
