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
 * <p>
 * If the lambda is only permitted to throw a specific exception and should handle everything
 * else internally, use a {@link ThrowingProcessor}.
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 * @see Callback
 * @see Producer
 * @see ThrowingProcessor
 */
@FunctionalInterface
public interface Processor<T, R> extends ThrowingProcessor<T, R, Exception> {
}
