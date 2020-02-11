/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

/**
 * Implements the {@link java.util.function.Supplier} pattern but permits the factory method to throw an exception.
 * <p>
 * This sometimes might simplify exception handling. If this feature is not required use a plain
 * {@link java.util.function.Supplier} instead.
 * <p>
 * If the lambda is only permitted to throw a specific exception and should handle everything
 * else internally, use a {@link ThrowingProducer}.
 *
 * @param <T> the type of results supplied by this producer
 * @see Callback
 * @see Processor
 * @see ThrowingProducer
 */
@FunctionalInterface
public interface Producer<T> extends ThrowingProducer<T, Exception> {

}
