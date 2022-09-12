/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel;

import sirius.kernel.di.std.AutoRegister;
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
@AutoRegister
public interface Killable extends Priorized {

    @Override
    default int getPriority() {
        return DEFAULT_PRIORITY;
    }

    /**
     * Called after {@link Stoppable#stopped()} has been called to wait until all tasks are fully finished.
     * <p>
     * This method may block for a certain amount of time to permit the subsystem to shut down properly. However,
     * it should not block infinitely...
     */
    void awaitTermination();
}
