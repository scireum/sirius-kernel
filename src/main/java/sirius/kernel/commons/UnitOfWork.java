/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

/**
 * Implements the {@link Runnable} pattern but permits the function to throw an exception.
 * <p>
 * This sometimes might simplify exception handling. If this feature is not required use a plain
 * {@link Runnable} instead.
 *
 * @see Producer
 * @see Callback
 * @see Processor
 */
public interface UnitOfWork {

    /**
     * Starts or re-starts the block of code.
     *
     * @throws Exception the callee may throw any exception during the computation. Therefore the caller should
     *                   implement proper error handling without relying on specific exception types.
     */
    void execute() throws Exception;
}
