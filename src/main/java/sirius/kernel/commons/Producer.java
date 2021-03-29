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
 *
 * @param <T> the type of results supplied by this producer
 * @see UnitOfWork
 * @see Callback
 * @see Processor
 */
@FunctionalInterface
public interface Producer<T> {

    /**
     * Gets a result.
     *
     * @return a result
     * @throws Exception the callee may throw any exception during the computation. Therefore the caller should
     *                   implement proper error handling without relying on specific exception types.
     */
    T create() throws Exception;
}
