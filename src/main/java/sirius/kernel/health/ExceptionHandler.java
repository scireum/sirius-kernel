/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health;

/**
 * Instances registered for this interface will be notified about every exception handled by {@link Exceptions}
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/11
 */
public interface ExceptionHandler {
    /**
     * Invoked to handle the given exception.
     * <p>
     * Can be used to get notified about any exception which occurs in the system.
     *
     * @param incident contains the error description to be processed.
     * @throws Exception as this method is already called from within the exception handling system, errors in here
     *                   should not be sent there again, but simply be thrown by this method
     * @see sirius.kernel.async.CallContext#getMDC()
     * @see sirius.kernel.health.Exceptions#handle()
     */
    void handle(Incident incident) throws Exception;
}
