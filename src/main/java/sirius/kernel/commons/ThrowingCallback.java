/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

/**
 * Implements the {@link java.util.function.Consumer} pattern which permits the handler to throw a specific exception.
 * <p>
 * This sometimes might simplify exception handling. If this feature is not required use a plain
 * {@link java.util.function.Consumer} instead. If any exception should be passed through use {@link Callback}.
 *
 * @param <T> the type of the object passed to the callback
 * @param <E> the type of exceptions being thrown by the inner lambda
 * @see Processor
 * @see Producer
 */
@FunctionalInterface
public interface ThrowingCallback<T, E extends Exception> {
    /**
     * Invokes the callback with <tt>value</tt>
     *
     * @param value the value to supply to the callback
     * @throws E might be thrown and should be handled by the outside code
     */
    void invoke(T value) throws E;
}
