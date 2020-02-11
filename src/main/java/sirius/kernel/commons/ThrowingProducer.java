/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

/**
 * Implements the {@link java.util.function.Supplier} pattern but permits the factory method to throw a specific exception.
 * <p>
 * This sometimes might simplify exception handling. If this feature is not required use a plain
 * {@link java.util.function.Supplier} instead.
 *
 * @param <T> the type of results supplied by this producer
 * @param <E> the type of exceptions being thrown by the inner lambda
 * @see Callback
 * @see Processor
 */
@FunctionalInterface
public interface ThrowingProducer<T, E extends Exception> {

    /**
     * Gets a result.
     *
     * @return a result
     * @throws E might be thrown and should be handled by the outside code
     */
    T create() throws E;
}
