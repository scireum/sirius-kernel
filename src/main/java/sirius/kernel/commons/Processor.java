/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

/**
 * Implements the {@link java.util.function.Function} pattern but permits the function to throw an exception.
 * <p>
 * This sometimes might simplify exception handling. If this feature is not required use a plain
 * {@link java.util.function.Function} instead.
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 *
 * @see UnitOfWork
 * @see Callback
 * @see Producer
 */
@FunctionalInterface
public interface Processor<T, R> {

    /**
     * Applies this function to the given argument.
     *
     * @param t the function argument
     * @return the function result
     * @throws Exception the callee may throw any exception during the computation. Therefore the caller should
     *                   implement proper error handling without relying on specific exception types.
     */
    R apply(T t) throws Exception;
}
