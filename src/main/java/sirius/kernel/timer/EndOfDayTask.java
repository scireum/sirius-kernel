/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.timer;

import sirius.kernel.commons.Timeout;
import sirius.kernel.di.std.AutoRegister;
import sirius.kernel.di.std.Register;

/**
 * Represents a cleanup or maintenance task which is executed in a daily manner.
 * <p>
 * Most systems need to perform cleanup tasks on a daily basis, which are executed during idle periods in the night.
 * However, implementing them a {@link EveryDay} requires the admin to pick an appropriate execution hour for each task.
 * <p>
 * All classes implementing this interface and wearing the {@link Register} annotation, will be discovered and executed
 * by the {@link EndOfDayTaskExecutor}. Processing will start at a defined time (10pm) by default and will continue
 * until 5am (including). This way all tasks are executed (one after another) so that the system isn't overloaded and
 * execution is automatically stopped once the daily workload rises.
 * <p>
 * Note that the list of tasks is shuffled before each execution so that no single task can jam up the execution
 * permanently.
 * <p>
 * Note that the {@link EndOfDayTaskExecutor} only checks if processing should continue before beginning with
 * the next task. If a single task has the possibility of running for a long period (e.g. greater than one hour),
 * the task itself should protect the system (e.g. by limiting its runtime via {@link Timeout}.
 */
@AutoRegister
public interface EndOfDayTask {

    /**
     * Returns a short name which is used for logging.
     * <p>
     * Note that the name shoudln't contain any whitespace so that the task can be started out of schedule by
     * invoking the {@link sirius.kernel.health.console.EndOfDayCommand eod command} in the console.
     *
     * @return the name of the task
     */
    String getName();

    /**
     * Actually executes the task.
     *
     * @throws Exception in case of any error during the execution
     */
    void execute() throws Exception;
}
