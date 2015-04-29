/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents a background worker which is constantly processing a set of tasks (if available).
 * <p>
 * Often a system need all kinds of cleanup jobs which run constantly in the background, without ever affecting
 * the system performance.
 * <p>
 * Therefore a <tt>BackgroundTaskQueue</tt> is called repeatedly to create task which is den executed. All the
 * work is done in one single thread, so that no other task is blocked. If no work is available, the
 * thread is put to sleep automatically.
 */
public interface BackgroundTaskQueue {

    /**
     * Returns the name of the queue.
     *
     * @return the name of the queue for reporting and logging
     */
    @Nonnull
    String getQueueName();

    /**
     * Retuns a unit of work to be executed by the background thread.
     *
     * @return a unit of work or <tt>null</tt> to indicate that currently no work is available
     */
    @Nullable
    Runnable getWork();
}
