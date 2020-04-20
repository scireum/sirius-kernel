/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

import sirius.kernel.Sirius;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Watch;
import sirius.kernel.di.GlobalContext;
import sirius.kernel.di.std.AutoRegister;
import sirius.kernel.di.std.Part;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;
import sirius.kernel.nls.NLS;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
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
@AutoRegister
public abstract class BackgroundLoop {

    private static final double EVERY_SECOND = 1;
    private static final double EVERY_TEN_SECONDS = 0.1;

    @Part
    private Tasks tasks;

    @Part
    @Nullable
    private Orchestration orchestration;

    @Part
    private static GlobalContext globalContext;

    private Future loopExecuted = new Future();
    private volatile boolean enabled = true;
    private long lastExecutionAttempt;
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
     *
     * @return a short description of what was done during the execution or <tt>null</tt> if nothing happened
     * @throws Exception in case of any error. The outer loop has a catch all rule to log exceptions.
     */
    @Nullable
    protected abstract String doWork() throws Exception;

    /**
     * Determines the maximal call frequency of {@link #doWork()} in Hertz (ticks per second).
     *
     * @return the maximal call frequency in Hertz.
     */
    public double maxCallFrequency() {
        return Sirius.isStartedAsTest() ? EVERY_SECOND : EVERY_TEN_SECONDS;
    }

    /**
     * Defines the maximal (expected) runtime for this loop.
     * <p>
     * By default, we use the {@link #maxCallFrequency()}, compute the period of this frequency and multiply it by 5
     * to have some safety margin.
     * <p>
     * For loops with other runtime behaviours, this method should be overwritten.
     *
     * @return the maximal expected runtime, which is by default {@code 1 / frequency * 5}
     */
    public double maxRuntimeInSeconds() {
        return 1d / maxCallFrequency() * 5;
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
        tasks.executor(determineExecutor()).frequency(this, maxCallFrequency()).start(this::tryExecuteWork);
    }

    private void tryExecuteWork() {
        try {
            lastExecutionAttempt = System.currentTimeMillis();
            if (shouldExecute()) {
                executeWork();
            }
        } catch (Exception e) {
            Exceptions.handle(Tasks.LOG, e);
        }

        loop();
    }

    private void executeWork() throws Exception {
        Future executionFuture = loopExecuted;
        loopExecuted = new Future();
        try {
            Watch w = Watch.start();
            LocalDateTime now = LocalDateTime.now();
            String executedWork = doWork();
            buildAndLogExecutionInfo(w, now, executedWork);
        } finally {
            if (orchestration != null) {
                orchestration.backgroundLoopCompleted(getName(), executionInfo);
            }
            executionFuture.success();
        }
    }

    private boolean shouldExecute() {
        if (!enabled) {
            return false;
        }

        if (!Sirius.isRunning()) {
            return false;
        }

        if (orchestration != null) {
            return orchestration.tryExecuteBackgroundLoop(getName());
        } else {
            return true;
        }
    }

    private void buildAndLogExecutionInfo(Watch watch, LocalDateTime startedAt, String executedWorkDescription) {
        if (executedWorkDescription != null) {
            executionInfo = NLS.toUserString(startedAt) + " (" + watch.duration() + "): " + executedWorkDescription;
            Log.BACKGROUND.FINE(getName() + ": " + executionInfo);
        } else {
            executionInfo = NLS.toUserString(startedAt) + " (" + watch.duration() + ") - no work executed...";
        }
    }

    @Override
    public String toString() {
        return "BackgroundLoop '"
               + getName()
               + "': "
               + getExecutionInfo()
               + " (Last execution attempt: "
               + NLS.toUserString(getLastExecutionAttempt())
               + ")";
    }

    /**
     * Contains the timestamp and duration of the last execution for monitoring purposes.
     *
     * @return a formatted timestamp and duration for logging and monitoring purposes.
     */
    public String getExecutionInfo() {
        return executionInfo;
    }

    /**
     * Contains the timestamp of the last execution (or execution attempt) of this loop to detect jams.
     *
     * @return the last timestamp when an execution was attempted
     */
    public Instant getLastExecutionAttempt() {
        return Instant.ofEpochMilli(lastExecutionAttempt);
    }

    /**
     * Returns the execution future of the given background loop.
     * <p>
     * This is only intended to be used by tests to await the execution of a background loop.
     *
     * @param type the type of the background loop to fetch the future from
     * @return the future which will be fulfilled after the next completion of the given background loop
     */
    public static Future nextExecution(Class<? extends BackgroundLoop> type) {
        if (!Sirius.isStartedAsTest()) {
            throw new IllegalStateException("BackgroundLoop.extExecution may only be called in tests.");
        }
        return findLoop(type).loopExecuted;
    }

    private static BackgroundLoop findLoop(Class<? extends BackgroundLoop> type) {
        return globalContext.getParts(BackgroundLoop.class)
                            .stream()
                            .filter(loop -> type.equals(loop.getClass()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException(Strings.apply("Unknown background loop: %s",
                                                                                          type)));
    }

    /**
     * Disables the background loop.
     * <p>
     * This is only intended to be used by tests to make sure a loop isn't executed while preparing a scenario.
     * <p>
     * Note that this call actually awaits the next run of the loop to then toggle the flag just in the right moment.
     * This is done to not add any locking overheads during production usage.
     *
     * @param type the loop to disable
     * @return a future which is completed once the loop is known to be disabled
     */
    public static Future disable(Class<? extends BackgroundLoop> type) {
        if (!Sirius.isStartedAsTest()) {
            throw new IllegalStateException("BackgroundLoop.disable may only be called in tests.");
        }

        BackgroundLoop loop = findLoop(type);

        return loop.loopExecuted.onSuccess(() -> {
            loop.enabled = false;
        });
    }

    /**
     * Enables the given background loop.
     * <p>
     * This is only intended to be used by tests which called {@link #disable(Class)} before.
     *
     * @param type the loop to enable
     */
    public static void enable(Class<? extends BackgroundLoop> type) {
        if (!Sirius.isStartedAsTest()) {
            throw new IllegalStateException("BackgroundLoop.enable may only be called in tests.");
        }

        findLoop(type).enabled = true;
    }

    /**
     * Executes the background loop out of order.
     * <p>
     * This is only intended to be used by tests. Note that the background loop must be {@link #disable(Class) disabled}
     * for this.
     *
     * @param type the loop to forcefully execute
     */
    public static void executeOutOfOrder(Class<? extends BackgroundLoop> type) {
        if (!Sirius.isStartedAsTest()) {
            throw new IllegalStateException("BackgroundLoop.executeOutOfOrder may only be called in tests.");
        }

        BackgroundLoop loop = findLoop(type);
        if (loop.enabled) {
            throw new IllegalStateException("BackgroundLoop.executeOutOfOrder may only be called for disabled loops");
        }

        try {
            loop.executeWork();
        } catch (Exception e) {
            Exceptions.handle(Tasks.LOG, e);
        }
    }
}
