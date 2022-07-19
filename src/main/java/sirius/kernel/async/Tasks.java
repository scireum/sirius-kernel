/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

import sirius.kernel.Killable;
import sirius.kernel.Sirius;
import sirius.kernel.Startable;
import sirius.kernel.Stoppable;
import sirius.kernel.commons.Explain;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Tuple;
import sirius.kernel.di.PartCollection;
import sirius.kernel.di.std.Parts;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;
import sirius.kernel.settings.Extension;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Static helper for managing and scheduling asynchronous background tasks.
 * <p>
 * Provides various helper methods to execute tasks in another thread and to provide interaction via instances of
 * {@link Promise}.
 * <p>
 * Scheduling tasks via {@link #executor(String)} or {@link #defaultExecutor()} provides externally configured
 * thread-pools (via <tt>async.executor</tt>) as well as auto transfer of the current {@link CallContext} to the
 * called thread.
 * <p>
 * Additionally helper-methods for creating and aggregating instances {@link Promise} are provided, which are the
 * main interaction model when dealing with async and non-blocking execution.
 */
@ParametersAreNonnullByDefault
@Register(classes = {Tasks.class, Startable.class, Stoppable.class, Killable.class})
public class Tasks implements Startable, Stoppable, Killable {

    /**
     * Contains the name of the default executor.
     */
    public static final String DEFAULT = "default";

    /**
     * Contains the priority of this lifecycle.
     */
    public static final int LIFECYCLE_PRIORITY = 25;

    protected static final Log LOG = Log.get("tasks");
    protected final Map<String, AsyncExecutor> executors = new ConcurrentHashMap<>();

    // If sirius is not started yet, we still consider it running already as the intention of this flag
    // is to detect a system halt and not to check if the startup sequence has finished.
    private volatile boolean running = true;

    @Parts(BackgroundLoop.class)
    private static PartCollection<BackgroundLoop> backgroundLoops;

    private final Map<Object, Long> scheduleTable = new ConcurrentHashMap<>();
    private final List<ExecutionBuilder.TaskWrapper> schedulerQueue = new ArrayList<>();
    private final Lock schedulerLock = new ReentrantLock();
    private final Condition workAvailable = schedulerLock.newCondition();

    /**
     * Determines the duration we wait for an executor to shut down normally
     * (after having called {@link ThreadPoolExecutor#shutdown()})
     */
    private static final Duration EXECUTOR_SHUTDOWN_WAIT = Duration.ofSeconds(60);

    /**
     * Determines the duration we wait for an executor to shut down forced
     * (after having called {@link ThreadPoolExecutor#shutdownNow()})
     */
    private static final Duration EXECUTOR_TERMINATION_WAIT = Duration.ofSeconds(30);

    /**
     * Returns the executor for the given category.
     * <p>
     * The configuration for this executor is taken from <tt>async.executor.[category]</tt>. If no config is found,
     * the default values are used.
     *
     * @param category the category of the task to be executed, which implies the executor to use.
     * @return the execution builder which submits tasks to the appropriate executor.
     */
    @Nonnull
    public ExecutionBuilder executor(String category) {
        return new ExecutionBuilder(this, category);
    }

    /**
     * Returns the default executor.
     *
     * @return the execution builder which submits tasks to the default executor.
     */
    public ExecutionBuilder defaultExecutor() {
        return new ExecutionBuilder(this, DEFAULT);
    }

    /**
     * Exposes the raw executor service for the given category.
     * <p>
     * This shouldn't be used for custom task scheduling (use {@link #executor(String)} instead) but rather for other
     * frameworks which need an executor service. Using this approach, all thread pools of an application are managed
     * and visible via central facility.
     *
     * @param category the category which is used to specify the capacity of the executor
     * @return the executor service for the given category
     */
    @Nonnull
    public AsyncExecutor executorService(String category) {
        return findExecutor(category);
    }

    /*
     * Executes a given TaskWrapper by fetching or creating the appropriate executor and submitting the wrapper.
     */
    protected void execute(ExecutionBuilder.TaskWrapper wrapper) {
        if (wrapper.synchronizer == null) {
            executeNow(wrapper);
        } else {
            schedule(wrapper);
        }
    }

    private void executeNow(ExecutionBuilder.TaskWrapper wrapper) {
        wrapper.prepare();
        AsyncExecutor exec = findExecutor(wrapper.category);
        wrapper.jobNumber = exec.executed.inc();
        wrapper.durationAverage = exec.duration;
        if (wrapper.synchronizer != null) {
            scheduleTable.put(wrapper.synchronizer, System.currentTimeMillis());
        }
        exec.execute(wrapper);
    }

    private AsyncExecutor findExecutor(String category) {
        return executors.computeIfAbsent(category, categoryName -> {
            Extension config = Sirius.getSettings().getExtension("async.executor", categoryName);
            return new AsyncExecutor(categoryName,
                                     config.get("poolSize").getInteger(),
                                     config.get("queueLength").getInteger());
        });
    }

    private synchronized void schedule(ExecutionBuilder.TaskWrapper wrapper) {
        // As tasks often create a loop by calling itself (e.g. BackgroundLoop), we drop
        // scheduled tasks if the async framework is no longer running, as the tasks would be rejected and
        // dropped anyway...
        if (!running) {
            return;
        }
        Long lastInvocation = scheduleTable.get(wrapper.synchronizer);
        if (lastInvocation == null || (System.currentTimeMillis() - lastInvocation) > wrapper.intervalMinLength) {
            executeNow(wrapper);
        } else {
            if (dropIfAlreadyScheduled(wrapper)) {
                if (LOG.isFINE()) {
                    LOG.FINE(
                            "Dropping a scheduled task (%s), as for its synchronizer (%s) another task is already scheduled",
                            wrapper.runnable,
                            wrapper.synchronizer);
                }
                return;
            }
            wrapper.waitUntil = lastInvocation + wrapper.intervalMinLength;
            addToSchedulerQueue(wrapper);
            wakeSchedulerLoop();
        }
    }

    private void addToSchedulerQueue(ExecutionBuilder.TaskWrapper wrapper) {
        // The scheduler queue is sorted by waitUntil -> add at correct position
        synchronized (schedulerQueue) {
            for (int index = 0; index < schedulerQueue.size(); index++) {
                if (schedulerQueue.get(index).waitUntil > wrapper.waitUntil) {
                    schedulerQueue.add(index, wrapper);
                    return;
                }
            }
            schedulerQueue.add(wrapper);
        }
    }

    private boolean dropIfAlreadyScheduled(ExecutionBuilder.TaskWrapper wrapper) {
        synchronized (schedulerQueue) {
            for (ExecutionBuilder.TaskWrapper other : schedulerQueue) {
                if (wrapper.synchronizer.equals(other.synchronizer)) {
                    wrapper.drop();
                    return true;
                }
            }
        }
        return false;
    }

    private void schedulerLoop() {
        while (running) {
            try {
                executeWaitingTasks();
                idle();
            } catch (Exception t) {
                Exceptions.handle(LOG, t);
            }
        }
    }

    private void executeWaitingTasks() {
        synchronized (schedulerQueue) {
            Iterator<ExecutionBuilder.TaskWrapper> iter = schedulerQueue.iterator();
            long now = System.currentTimeMillis();
            while (iter.hasNext()) {
                ExecutionBuilder.TaskWrapper wrapper = iter.next();
                if (wrapper.waitUntil <= now) {
                    executeNow(wrapper);
                    iter.remove();
                } else {
                    // The scheduler queue is sorted by "waitUntil" -> as soon as we discover the
                    // first task which can not run yet, we can abort...
                    return;
                }
            }
        }
    }

    @SuppressWarnings({"squid:S2274", "squid:S899"})
    @Explain("We neither need a loop nor the result here.")
    private void idle() {
        try {
            schedulerLock.lock();
            try {
                long waitTime = computeWaitTime();
                if (waitTime < 0) {
                    // No work available -> wait for something to do...
                    workAvailable.await();
                } else if (waitTime > 0) {
                    // No task can be executed in the next millisecond. Sleep for
                    // "waitTime" (or more work) until the next check for executable work...
                    workAvailable.await(waitTime, TimeUnit.MILLISECONDS);
                }
            } finally {
                schedulerLock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Exceptions.ignore(e);
        }
    }

    private long computeWaitTime() {
        synchronized (schedulerQueue) {
            if (schedulerQueue.isEmpty()) {
                // No task is waiting -> wait forever...
                return -1;
            }

            long now = System.currentTimeMillis();
            return Math.max(0, schedulerQueue.get(0).waitUntil - now);
        }
    }

    private void startScheduler() {
        Thread schedulerThread = new Thread(this::schedulerLoop);
        schedulerThread.setName("TaskScheduler");
        schedulerThread.start();
    }

    private void wakeSchedulerLoop() {
        schedulerLock.lock();
        try {
            workAvailable.signalAll();
        } finally {
            schedulerLock.unlock();
        }
    }

    /**
     * Forks the given computation and returns a {@link Promise} for the computed value.
     * <p>
     * Forks the computation, which means that the current <tt>CallContext</tt> is transferred to the new thread,
     * and returns the result of the computation as promise.
     * <p>
     * If the executor for the given category is saturated (all threads are active and the queue is full) this
     * will drop the computation and the promise will be sent a <tt>RejectedExecutionException</tt>.
     *
     * @param category    the category which implies which executor to use.
     * @param computation the computation which eventually yields the resulting value
     * @param <V>         the type of the resulting value
     * @return a promise which will either be eventually supplied with the result of the computation or with an error
     */
    public <V> Promise<V> fork(String category, final Supplier<V> computation) {
        final Promise<V> result = new Promise<>();

        executor(category).dropOnOverload(() -> result.fail(new RejectedExecutionException())).fork(() -> {
            try {
                result.success(computation.get());
            } catch (Exception t) {
                result.fail(t);
            }
        });

        return result;
    }

    /**
     * Returns a list of all known executors.
     *
     * @return a collection of all executors which have been used by the system.
     */
    public Collection<AsyncExecutor> getExecutors() {
        return Collections.unmodifiableCollection(executors.values());
    }

    /**
     * Determines if the application is still running.
     * <p>
     * Can be used for long loops in async tasks to determine if a computation should be interrupted.
     *
     * @return <tt>true</tt> if the system is running (straight from the start), <tt>false</tt> if a shutdown is in progress
     * @see Sirius#isRunning() Provides a similar flag with slightly different semantics
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Returns a list containing the name and estimated execution time of all scheduled tasks which
     * are waiting for their execution.
     * <p>
     * A scheduled task in this case is one which has
     * {@link sirius.kernel.async.ExecutionBuilder.TaskWrapper#minInterval(Object, Duration)} or
     * {@link sirius.kernel.async.ExecutionBuilder.TaskWrapper#frequency(Object, double)} set.
     *
     * @return a list of all scheduled tasks
     */
    public List<Tuple<String, LocalDateTime>> getScheduledTasks() {
        synchronized (schedulerQueue) {
            List<Tuple<String, LocalDateTime>> result = new ArrayList<>();
            for (ExecutionBuilder.TaskWrapper wrapper : schedulerQueue) {
                result.add(Tuple.create(wrapper.category + " / " + wrapper.synchronizer.getClass().getName(),
                                        LocalDateTime.ofInstant(Instant.ofEpochMilli(wrapper.waitUntil),
                                                                ZoneId.systemDefault())));
            }
            return result;
        }
    }

    @Override
    public void started() {
        running = true;
        startScheduler();
        startBackgroundLoops();
    }

    private void startBackgroundLoops() {
        backgroundLoops.forEach(BackgroundLoop::loop);
    }

    @Override
    public void stopped() {
        running = false;
        wakeSchedulerLoop();
        // Try an ordered (fair) shutdown...
        for (AsyncExecutor exec : executors.values()) {
            exec.shutdown();
        }
    }

    @Override
    public void awaitTermination() {
        for (Map.Entry<String, AsyncExecutor> e : executors.entrySet()) {
            AsyncExecutor exec = e.getValue();
            if (!exec.isTerminated()) {
                blockUntilExecutorTerminates(e.getKey(), exec);
            }
        }
        executors.clear();
    }

    private void blockUntilExecutorTerminates(String name, AsyncExecutor exec) {
        LOG.INFO("Waiting for async executor '%s' to terminate...", name);
        try {
            if (!exec.awaitTermination(EXECUTOR_SHUTDOWN_WAIT.getSeconds(), TimeUnit.SECONDS)) {
                LOG.SEVERE(Strings.apply("Executor '%s' did not terminate within 60s. Interrupting " + "tasks...",
                                         name));
                exec.shutdownNow();
                if (!exec.awaitTermination(EXECUTOR_TERMINATION_WAIT.getSeconds(), TimeUnit.SECONDS)) {
                    LOG.SEVERE(Strings.apply("Executor '%s' did not terminate after another 30s!", name));
                }
            }
        } catch (InterruptedException ex) {
            Exceptions.ignore(ex);
            Thread.currentThread().interrupt();
            LOG.SEVERE(Strings.apply("Interrupted while waiting for '%s' to terminate!", name));
        }
    }

    @Override
    public int getPriority() {
        return LIFECYCLE_PRIORITY;
    }
}
