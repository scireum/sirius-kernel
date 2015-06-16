/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import sirius.kernel.commons.Strings;
import sirius.kernel.health.Average;
import sirius.kernel.health.Counter;
import sirius.kernel.health.Exceptions;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Represents an executor used by sirius to schedule background tasks.
 * <p>
 * Instances of this class are created and managed by {@link Tasks}. This class is only made public so it can be
 * accessed for statistical reasons like ({@link #getBlocked()} or {@link #getDropped()}.
 */
public class AsyncExecutor extends ThreadPoolExecutor implements RejectedExecutionHandler {

    private String category;
    private Counter blocked = new Counter();
    private Counter dropped = new Counter();
    protected Counter executed = new Counter();
    protected Average duration = new Average();

    private static final long DEFAULT_KEEP_ALIVE_TIME = 10;

    AsyncExecutor(String category, int poolSize, int queueLength) {
        super(poolSize, poolSize, DEFAULT_KEEP_ALIVE_TIME, TimeUnit.SECONDS, createWorkQueue(queueLength));
        this.category = category;
        setThreadFactory(new ThreadFactoryBuilder().setNameFormat(category + "-%d").build());
        setRejectedExecutionHandler(this);
    }

    private static BlockingQueue<Runnable> createWorkQueue(int queueLength) {
        // Create queue with the given max. queue length
        if (queueLength > 0) {
            return new LinkedBlockingQueue<>(queueLength);
        }
        // Create a queue which will not hold any elements (no work queue)
        if (queueLength < 0) {
            return new SynchronousQueue<>();
        }

        // Create an unbounded queue
        return new LinkedBlockingQueue<>();
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        try {
            ExecutionBuilder.TaskWrapper wrapper = (ExecutionBuilder.TaskWrapper) r;
            if (wrapper.dropHandler != null) {
                wrapper.drop();
                dropped.inc();
            } else if (wrapper.synchronizer == null) {
                CallContext current = CallContext.getCurrent();
                try {
                    wrapper.run();
                } finally {
                    CallContext.setCurrent(current);
                }
                blocked.inc();
            } else {
                Exceptions.handle()
                          .to(Tasks.LOG)
                          .withSystemErrorMessage(
                                  "The execution of a frequency scheduled task '%s' (%s) synchronized on '%s' was rejected by: %s - Aborting!",
                                  wrapper.runnable,
                                  wrapper.runnable.getClass(),
                                  wrapper.synchronizer,
                                  category)
                          .handle();
            }
        } catch (Throwable t) {
            Exceptions.handle(Tasks.LOG, t);
        }
    }

    @Override
    public String toString() {
        return Strings.apply("%s - Active: %d, Queued: %d, Executed: %d, Blocked: %d, Rejected: %d",
                             category,
                             getActiveCount(),
                             getQueue().size(),
                             executed.getCount(),
                             blocked.getCount(),
                             dropped.getCount());
    }

    /**
     * Returns the category this executor was created for.
     *
     * @return the category of tasks this executor runs.
     */
    public String getCategory() {
        return category;
    }

    /**
     * The number of tasks which were executed by this executor
     *
     * @return the number of tasks executed so far
     */
    public long getExecuted() {
        return executed.getCount();
    }

    /**
     * The average duration of a task in milliseconds.
     *
     * @return the average execution time of a task in milliseconds
     */
    public double getAverageDuration() {
        return duration.getAvg();
    }

    /**
     * The number of tasks which were executed by blocking the caller due to system overload conditions.
     * <p>
     * A system overload occurs if all available tasks are busy and the queue of this executor reached its limit.
     *
     * @return the number of blocking task executions so far.
     */
    public long getBlocked() {
        return blocked.getCount();
    }

    /**
     * The number of tasks dropped due to system overload conditions.
     * <p>
     * A system overload occurs if all available tasks are busy and the queue of this executor reached its limit. If
     * a task has a <tt>dropHandler</tt> attached, the handler is informed and the task is not executed by simply
     * deleted.
     *
     * @return the number of dropped tasks so far.
     */
    public long getDropped() {
        return dropped.getCount();
    }
}
