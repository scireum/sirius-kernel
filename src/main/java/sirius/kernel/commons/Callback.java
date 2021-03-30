/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

/**
 * Implements the {@link java.util.function.Consumer} pattern which permits the handler to throw an exception.
 * <p>
 * This sometimes might simplify exception handling. If this feature is not required use a plain
 * {@link java.util.function.Consumer} instead.
 *
 * @param <T> the type of the object passed to the callback
 * @see UnitOfWork
 * @see Processor
 * @see Producer
 */
@FunctionalInterface
public interface Callback<T> {
    /**
     * Invokes the callback with <tt>value</tt>
     *
     * @param value the value to supply to the callback.
     * @throws Exception the callee may throw any exception during the computation. Therefore the caller should
     *                   implement proper error handling without relying on specific exception types.
     */
    void invoke(T value) throws Exception;
}
