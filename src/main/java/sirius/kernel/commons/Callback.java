/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

/**
 * Provides a simple callback which can be invoked with a value of the given type.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public interface Callback<T> {
    /**
     * Invokes the callback with <tt>value</tt>
     *
     * @param value the value to supply to the callback.
     * @throws Exception The callee may throw any exception during the computation. Therefore the caller should
     *                   implement proper error handling without relying on specific exception types.
     */
    void invoke(T value) throws Exception;
}
