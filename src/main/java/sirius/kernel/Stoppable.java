/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel;

import sirius.kernel.di.std.Priorized;

/**
 * Classes implementing this interface get notified once the framework is being shut down.
 * <p>
 * The framework lifecycle is split into three phases:
 * <ul>
 * <li>{@link Startable}: Each startable component is invoked during startup.</li>
 * <li>{@link Stoppable}: Each stoppable component is invoked during framework shutdown.</li>
 * <li>{@link Killable}: The the shutdown process has to wait for a task to finish, this can be used to block until a
 * task is completed.</li>
 * </ul>
 */
public interface Stoppable extends Priorized {

    @Override
    default int getPriority() {
        return DEFAULT_PRIORITY;
    }

    /**
     * Invoked when the framework shuts down.
     * <p>
     * This method must not block (and wait for internals to stop). This can be done in
     * {@link Killable#awaitTermination()}.
     */
    void stopped();
}
