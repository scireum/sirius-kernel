/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

import sirius.kernel.commons.Watch;
import sirius.kernel.di.std.Part;
import sirius.kernel.health.Exceptions;
import sirius.kernel.nls.NLS;

import javax.annotation.Nonnull;
import java.time.LocalDateTime;

/**
 * Represents a background worker which is constantly processing a set of tasks (if available).
 * <p>
 * Often a system needs all kinds of cleanup jobs which run constantly in the background, without ever affecting
 * the system performance.
 * <p>
 * Therefore a <tt>BackgroundLoop</tt> is called repeatedly to perform some work. If no work is available the
 * loop automatically throttles itself to a certain frequency so that no system resources are wasted.
 * <p>
 * By default the {@link #doWork()} method is at most executed every ten seconds. The can be changed by overriding
 * {@link #maxCallFrequency()}.
 * <p>
 * Note that subclasses must wear an {@link sirius.kernel.di.std.Register} annotation like this:
 * {@code @Register(classes = BackgroundLoop.class)} to be visible to the framework.
 */
public abstract class BackgroundLoop {

    @Part
    private Tasks tasks;

    private String executionInfo = "-";

    /**
     * Returns the name of the loop.
     *
     * @return the name of the loop for reporting and logging
     */
    @Nonnull
    public abstract String getName();

    /**
     * Executes the actual work.
     */
    protected abstract void doWork() throws Exception;

    /**
     * Determines the maximal call frequency of {@link #doWork()} in Herz (ticks per second).
     *
     * @return the maximal call frequency in Herz.
     */
    protected double maxCallFrequency() {
        return 0.1;
    }

    /**
     * Determines the executor (thread pool) used to execute the actual work.
     * <p>
     * By default the <tt>background</tt> executor is used.
     *
     * @return the name of the executor to use when calling {@code doWork}.
     */
    @Nonnull
    protected String determineExecutor() {
        return "background";
    }

    /**
     * Calls {@link #executeWork()} in the determined executor.
     * <p>
     * This is kind of the main loop, as {@code executeWork()} will call {@code loop()} once the computation
     * ({@code doWork()} is finished. Using {@link ExecutionBuilder#frequency(Object, double)} this is limited to the
     * call frequency as determined by {@code maxCallFrequency()}.
     */
    protected void loop() {
        tasks.executor(determineExecutor()).frequency(this, maxCallFrequency()).start(this::executeWork);
    }

    /**
     * Calls {@code doWork()} with proper error handling and then {@code loop()} again to schedule the next call.
     */
    private void executeWork() {
        try {
            Watch w = Watch.start();
            LocalDateTime now = LocalDateTime.now();
            doWork();
            executionInfo = NLS.toUserString(now) + " (" + w.duration() + ")";
        } catch (Throwable e) {
            Exceptions.handle(Tasks.LOG, e);
        }
        loop();
    }

    @Override
    public String toString() {
        return "BackgroundLoop '" + getName() + "': " + getExecutionInfo();
    }

    /**
     * Contains the timestamp and duration of the last execution for monitoring purposes.
     *
     * @return a formatted timestamp and duration for logging and monitoring purposes.
     */
    public String getExecutionInfo() {
        return executionInfo;
    }
}
