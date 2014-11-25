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
import sirius.kernel.health.Exceptions;

import java.util.concurrent.*;

/**
 * Represents an executor used by sirius to schedule background tasks.
 * <p>
 * Instances of this class are created and managed by {@link Async}. This class is only made public so it can be
 * accessed for statistical reasons like ({@link #getBlocked()} or {@link #getDropped()}.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public class AsyncExecutor extends ThreadPoolExecutor implements RejectedExecutionHandler {

    private ThreadPoolExecutor executor;
    private String category;
    private int blocked;
    private int dropped;

    AsyncExecutor(String category, int poolSize, int queueLength) {
        super(poolSize,
              poolSize,
              10L,
              TimeUnit.SECONDS,
              queueLength > 0 ? new LinkedBlockingQueue<>(queueLength) : new LinkedBlockingQueue<>());
        this.category = category;
        setThreadFactory(new ThreadFactoryBuilder().setNameFormat(category + "-%d").build());
        setRejectedExecutionHandler(this);
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        try {
            ExecutionBuilder.TaskWrapper wrapper = (ExecutionBuilder.TaskWrapper) r;
            if (wrapper.dropHandler != null) {
                wrapper.dropHandler.run();
                wrapper.promise.fail(new RejectedExecutionException());
                dropped++;
            } else {
                wrapper.run();
                blocked++;
            }
        } catch (Throwable t) {
            Exceptions.handle(Async.LOG, t);
        }
    }

    @Override
    public String toString() {
        return Strings.apply("%s - Active: %d, Queued: %d, Executed: %d, Blocked: %d, Rejected: %d",
                             category,
                             getActiveCount(),
                             getQueue().size(),
                             getCompletedTaskCount(),
                             blocked,
                             dropped);
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
     * The number of tasks which were executed by blocking the caller due to system overload conditions.
     * <p>
     * A system overload occurs if all available tasks are busy and the queue of this executor reached its limit.
     * </p>
     *
     * @return the number of blocking task executions so far.
     */
    public int getBlocked() {
        return blocked;
    }

    /**
     * The number of tasks dropped due to system overload conditions.
     * <p>
     * A system overload occurs if all available tasks are busy and the queue of this executor reached its limit. If
     * a task has a <tt>dropHandler</tt> attached, the handler is informed and the task is not executed by simply
     * deleted.
     * </p>
     *
     * @return the number of dropped tasks so far.
     */
    public int getDropped() {
        return dropped;
    }
}
