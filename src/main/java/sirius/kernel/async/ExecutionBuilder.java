/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

import sirius.kernel.commons.Watch;
import sirius.kernel.health.Average;
import sirius.kernel.health.Exceptions;

import javax.annotation.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;
import java.time.Duration;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Builder pattern for forking or starting sub tasks.
 * <p>
 * Used by {@link Tasks#executor(String)} to construct an execution for a given subtask. Can be used to specify whether
 * the current {@link CallContext} is forked, or if a new one is started. Also one can specify whether and how a given
 * task might be dropped on system overload conditions.
 * <p>
 * Most of the time this builder will be used to either call {@link #fork(Runnable)} or {@link #start(Runnable)}
 * to either fork the current <tt>CallContext</tt> or to start a sub task with a new one. Also a drop handler can be
 * supplied using {@link #dropOnOverload(Runnable)} to gracefully handle system overload conditions.
 */
@ParametersAreNonnullByDefault
public class ExecutionBuilder {

    private static final String SYSTEM_ASYNC = "ASYNC";
    private final Tasks tasks;

    /**
     * Internal class which takes care of passing along the CallContext and for storing the configuration made by the
     * ExecutionBuilder.
     */
    static class TaskWrapper implements Runnable {
        /**
         * Determines the executor (thread pool to use)
         */
        String category;

        /**
         * Contains the work to do
         */
        Runnable runnable;

        /**
         * Keep the current {@link CallContext} or create a new one?
         */
        boolean fork;

        /**
         * What to do if the we drop the task because the system is overloaded
         */
        Runnable dropHandler;

        /**
         * The {@code CallContext} to use when {@code fork} is <tt>true</tt>.
         */
        CallContext ctx;

        /**
         * Used to communicate the completion / abort of the task
         */
        Future promise = new Future();

        /**
         * Contains the inertnally computed task number
         */
        long jobNumber;

        /**
         * Filled by {@link Tasks#execute(TaskWrapper)} with the appropriate {@code Average} of the
         * underlying executor to measure throughput.
         */
        Average durationAverage;

        /**
         * If a synchorinzer is present this can be used to limit the call frequency of a task
         */
        long intervalMinLength;

        /**
         * Once the timestamp of the last invocation is known, this will contain the earliest possible
         * timestamp to execute this task
         */
        long waitUntil;

        /**
         * The synchronizer to use when {@code frequency} is set
         */
        Object synchronizer;

        /**
         * Prepares the execution of this task while checking all preconditions.
         */
        void prepare() {
            if (fork) {
                ctx = CallContext.getCurrent().fork();
            }
            if (runnable == null) {
                throw new IllegalArgumentException("Please provide a runnable for me to execute!");
            }
        }

        @Override
        public void run() {
            try {
                Watch w = Watch.start();
                try {
                    if (ctx == null) {
                        CallContext.initialize();
                    } else {
                        CallContext.setCurrent(ctx);
                    }
                    TaskContext.get().setSystem(SYSTEM_ASYNC).setSubSystem(category).setJob(String.valueOf(jobNumber));
                    runnable.run();
                    promise.success(null);
                } finally {
                    CallContext.detach();
                    durationAverage.addValue(w.elapsedMillis());
                }
            } catch (Exception t) {
                Exceptions.handle(Tasks.LOG, t);
                promise.fail(t);
            }
        }

        @Override
        public String toString() {
            return category;
        }

        protected void drop() {
            if (dropHandler != null) {
                dropHandler.run();
            }
            promise.doNotLogErrors();
            promise.fail(new RejectedExecutionException());
        }
    }

    private final TaskWrapper wrapper = new TaskWrapper();

    /**
     * Generates a new ExecutionBuilder for the given category.
     *
     * @param category the category which is used to determine which executor to use
     */
    ExecutionBuilder(Tasks tasks, String category) {
        this.tasks = tasks;
        wrapper.category = category;
    }

    /**
     * Specifies to fork the current CallContext while executing the given task.
     *
     * @param task the task to execute.
     * @return a {@code Future} representing the execution created by this builder.
     */
    public Future fork(Runnable task) {
        wrapper.runnable = task;
        wrapper.fork = true;
        tasks.execute(wrapper);
        return wrapper.promise;
    }

    /**
     * Specifies to create a new CallContext while executing the given task.
     *
     * @param task the task to execute.
     * @return a {@code Future} representing the execution created by this builder.
     */
    public Future start(Runnable task) {
        wrapper.runnable = task;
        wrapper.fork = false;
        tasks.execute(wrapper);
        return wrapper.promise;
    }

    /**
     * Specifies that the given task can be dropped (ignored) in system overload conditions, if at least the given
     * handler is called.
     *
     * @param dropHandler the handler which is informed if the task is dropped due to system overload conditions.
     * @return this for fluent builder calls.
     */
    @CheckReturnValue
    public ExecutionBuilder dropOnOverload(Runnable dropHandler) {
        wrapper.dropHandler = dropHandler;
        return this;
    }

    /**
     * Determines the minimal interval which has to elapse between two consecutive tasks scheduled for the given
     * {@code synchronizer}.
     * <p>
     * If the execution is requested 'too early' the scheduler will put the task into a queue and defer its execution.
     * If a task for the same synchronizer is deferred already, this task will be dropped completely.
     *
     * @param synchronizer            the object to synchronize on
     * @param minimalIntervalDuration the minimal duration of the interval
     * @return this for fluent builder calls.
     */
    public ExecutionBuilder minInterval(Object synchronizer, Duration minimalIntervalDuration) {
        this.wrapper.intervalMinLength =
                TimeUnit.SECONDS.toMillis(minimalIntervalDuration.getSeconds()) + TimeUnit.NANOSECONDS.toMillis(
                        minimalIntervalDuration.getNano());
        this.wrapper.synchronizer = synchronizer;
        return this;
    }

    /**
     * Determines the maximal call frequency for tasks scheduled for the given {@code synchronizer}.
     * <p>
     * If the execution is requested 'too early' the scheduler will put the task into a queue and defer its execution.
     * If a task for the same synchronizer is deferred already, this task will be dropped completely.
     *
     * @param synchronizer   the object to synchronize on
     * @param ticksPerSecond the call frequency in Hertz.
     * @return this for fluent builder calls.
     */
    public ExecutionBuilder frequency(Object synchronizer, double ticksPerSecond) {
        return minInterval(synchronizer, Duration.ofNanos(Math.round(1_000_000_000d / ticksPerSecond)));
    }
}
