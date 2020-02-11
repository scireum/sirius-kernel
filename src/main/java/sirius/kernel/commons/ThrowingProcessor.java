/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

/**
 * Implements the {@link java.util.function.Function} pattern but permits the function to throw a specific exception.
 * <p>
 * This sometimes might simplify exception handling. If this feature is not required use a plain
 * {@link java.util.function.Function} instead. If any exception should be passed through use {@link Processor}.
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 * @param <E> the type of exceptions being thrown by the inner lambda
 * @see Callback
 * @see Producer
 * @see Processor
 */
@FunctionalInterface
public interface ThrowingProcessor<T, R, E extends Exception> {

    /**
     * Applies this function to the given argument.
     *
     * @param t the function argument
     * @return the function result
     * @throws E might be thrown and should be handled by the outside code
     */
    R apply(T t) throws E;
}
