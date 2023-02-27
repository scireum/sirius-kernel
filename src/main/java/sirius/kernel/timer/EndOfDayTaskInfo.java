/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.timer;

import sirius.kernel.nls.NLS;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Provides some statistics about the execution of an {@link EndOfDayTask}.
 */
public class EndOfDayTaskInfo {

    protected EndOfDayTask task;
    protected LocalDateTime lastExecution;
    protected long lastDuration;
    protected boolean lastExecutionWasSuccessful;
    protected String lastErrorMessage;

    /**
     * Returns the associated task.
     *
     * @return the task represented by this info object
     */
    public EndOfDayTask getTask() {
        return task;
    }

    /**
     * Returns the last execution timestamp
     *
     * @return the day and time of the last execution
     */
    public LocalDateTime getLastExecution() {
        return lastExecution;
    }

    /**
     * Returns the last duration.
     *
     * @return the execution duration in millis
     */
    public long getLastDuration() {
        return lastDuration;
    }

    /**
     * Returns a formatted representation of the duration.
     *
     * @return a user readable string representing the last execution duration
     */
    public String getFormattedLastDuration() {
        return NLS.convertDuration(Duration.ofMillis(lastDuration), true, true);
    }

    /**
     * Determines if the last execution was successful.
     *
     * @return <tt>true</tt> if the last execution was successful, <tt>false</tt> otherwise
     */
    public boolean isLastExecutionWasSuccessful() {
        return lastExecutionWasSuccessful;
    }

    /**
     * Returns the error message of the last recorded error.
     *
     * @return the error message of the last error or "-" if the last execution was successful
     */
    public String getLastErrorMessage() {
        return lastErrorMessage != null ? lastErrorMessage : "-";
    }
}
