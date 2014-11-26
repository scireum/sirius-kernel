/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health;

/**
 * Can be used to "tap" the logging system.
 * <p>
 * A <tt>LogTap</tt> can re supplied to the {@link Log} class to be notified about all logged statements.
 */
public interface LogTap {
    /**
     * Invoked once a log message is received.
     *
     * @param message the message to log
     */
    void handleLogMessage(LogMessage message);
}
