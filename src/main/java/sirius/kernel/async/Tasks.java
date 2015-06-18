/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import sirius.kernel.Lifecycle;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.PartCollection;
import sirius.kernel.di.std.Parts;
import sirius.kernel.di.std.Register;
import sirius.kernel.extensions.Extension;
import sirius.kernel.extensions.Extensions;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
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
@Register(classes = {Tasks.class, Lifecycle.class})
public class Tasks implements Lifecycle {

    /**
     * Contains the name of the default executor.
     */
    public static final String DEFAULT = "default";

    /**
     * Contains the priority of this {@link Lifecycle}
     */
    public static final int LIFECYCLE_PRIORITY = 25;

    protected static final Log LOG = Log.get("tasks");
    protected final Map<String, AsyncExecutor> executors = Maps.newConcurrentMap();

    // If sirius is not started yet, we still consider it running already as the intention of this flag
    // is to detect a system halt and not to check if the startup sequence has finished.
    private volatile boolean running = true;

    @Parts(BackgroundLoop.class)
    private static PartCollection<BackgroundLoop> backgroundLoops;

    /**
     * Returns the executor for the given category.
     * <p>
     * The configuration for this executor is taken from <tt>async.executor.[category]</tt>. If no config is found,
     * the default values are used.
     *
     * @param category the category of the task to be executed, which implies the executor to use.
     * @return the execution builder which submits tasks to the appropriate executor.
     */
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
            scheduleTable.put(wrapper.synchronizer, System.nanoTime());
        }
        exec.execute(wrapper);
    }

    private AsyncExecutor findExecutor(String category) {
        AsyncExecutor exec = executors.get(category);
        if (exec == null) {
            synchronized (executors) {
                exec = executors.get(category);
                if (exec == null) {
                    Extension config = Extensions.getExtension("async.executor", category);
                    exec = new AsyncExecutor(category,
                                             config.get("poolSize").getInteger(),
                                             config.get("queueLength").getInteger());
                    executors.put(category, exec);
                }
            }
        }
        return exec;
    }

    private final Map<Object, Long> scheduleTable = new ConcurrentHashMap<>();
    private final List<ExecutionBuilder.TaskWrapper> schedulerQueue = Lists.newArrayList();
    private final Lock schedulerLock = new ReentrantLock();
    private final Condition workAvailable = schedulerLock.newCondition();

    private synchronized void schedule(ExecutionBuilder.TaskWrapper wrapper) {
        // As tasks often create a loop by calling itself (e.g. BackgroundLoop), we drop
        // scheduled tasks if the async framework is no longer running, as the tasks would be rejected and
        // dropped anyway...
        if (!running) {
            return;
        }
        Long lastInvocation = scheduleTable.get(wrapper.synchronizer);
        if (lastInvocation == null || (System.nanoTime() - lastInvocation) > wrapper.intervalMinLength) {
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
            } catch (Throwable t) {
                Exceptions.handle(LOG, t);
            }
        }
    }

    private void executeWaitingTasks() {
        synchronized (schedulerQueue) {
            Iterator<ExecutionBuilder.TaskWrapper> iter = schedulerQueue.iterator();
            long now = System.nanoTime();
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
                    // "waitTime" (or more work) unteil the next check for executable work...
                    workAvailable.await(waitTime, TimeUnit.MILLISECONDS);
                }
            } finally {
                schedulerLock.unlock();
            }
        } catch (InterruptedException e) {
            Exceptions.ignore(e);
        }
    }

    private long computeWaitTime() {
        synchronized (schedulerQueue) {
            if (schedulerQueue.isEmpty()) {
                // No task is waiting -> wait forever...
                return -1;
            }
            long now = System.nanoTime();

            // The first task in the scheduler queue is the next to be executed. Compute how long
            // we can idle before we must start the execution. As we convert from nano seconds to millis,
            // this method might return 0 (if the next task can be run in less than 1 ms). In this case
            // we do not idle at all and directly check for executable tasks again.
            return TimeUnit.NANOSECONDS.toMillis(schedulerQueue.get(0).waitUntil - now);
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
        final Promise<V> result = promise();

        executor(category).dropOnOverload(() -> result.fail(new RejectedExecutionException())).fork(() -> {
            try {
                result.success(computation.get());
            } catch (Throwable t) {
                result.fail(t);
            }
        });

        return result;
    }

    /**
     * Creates a new promise of the given type.
     *
     * @param <V> the type of the result which is promised.
     * @return a new <tt>Promise</tt> which can be used to represent an asynchronously computed value.
     */
    public static <V> Promise<V> promise() {
        return new Promise<>();
    }

    /**
     * Creates a new future, which is just an untyped <tt>Promise</tt> where the completion is more important than
     * the actual result of the computation.
     *
     * @return a new <tt>Future</tt> which can be used to wait for a computation without a specific result.
     */
    public static Future future() {
        return new Future();
    }

    /**
     * Generates a promise which is immediately successful.
     *
     * @param value the value which is used as promised result
     * @param <V>   the type of the promise (or its promised value)
     * @return a promise which is already completed with the given value.
     */
    public static <V> Promise<V> success(@Nullable V value) {
        Promise<V> result = promise();
        result.success(value);
        return result;
    }

    /**
     * Generates a promise which has immediately failed.
     *
     * @param ex  the error which is used as failure
     * @param <V> the type of the promise (or its promised value)
     * @return a promise which is already completed, but failed with the given error.
     */
    public static <V> Promise<V> fail(Throwable ex) {
        Promise<V> result = promise();
        result.fail(ex);
        return result;
    }

    /**
     * Turns a list of promises into a promise for a list of values.
     * <p>
     * Note that all values need to have the same type.
     * <p>
     * If only the completion of all promises matters in contrast to their actual result, a {@link Barrier} can also
     * be used. This permits to wait for promises of different types.
     *
     * @param list the list of promises to convert.
     * @param <V>  the type of each promise.
     * @return the promise which will complete if all promises completed or if at least on failed.
     */
    public static <V> Promise<List<V>> sequence(List<Promise<V>> list) {
        final Promise<List<V>> result = promise();

        // Create a list with the correct length
        final List<V> resultList = new ArrayList<V>();
        for (int i = 0; i < list.size(); i++) {
            resultList.add(null);
        }

        // Keep track when we're finished
        final CountDownLatch latch = new CountDownLatch(list.size());

        // Iterate over all promises and create a completion handler, which either forwards a failure or which placesy
        // a successfully computed in the created result list
        int index = 0;
        for (Promise<V> promise : list) {
            final int currentIndex = index;
            promise.onComplete(new CompletionHandler<V>() {
                @Override
                public void onSuccess(@Nullable V value) throws Exception {
                    if (!result.isFailed()) {
                        // onSuccess can be called from any thread -> sync on resultList...
                        synchronized (resultList) {
                            resultList.set(currentIndex, value);
                        }

                        // Keep track how many results we're waiting for and forward the result when we're finished.
                        latch.countDown();
                        if (latch.getCount() <= 0) {
                            result.success(resultList);
                        }
                    }
                }

                @Override
                public void onFailure(Throwable throwable) throws Exception {
                    result.fail(throwable);
                }
            });
            index++;
        }

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
     * @return <tt>true</tt> if the system is running, <tt>false</tt> if a shutdown is in progress
     */
    public boolean isRunning() {
        return running;
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

    @Override
    public void awaitTermination() {
        for (Map.Entry<String, AsyncExecutor> e : executors.entrySet()) {
            AsyncExecutor exec = e.getValue();
            if (!exec.isTerminated()) {
                LOG.INFO("Waiting for async executor '%s' to terminate...", e.getKey());
                try {
                    if (!exec.awaitTermination(EXECUTOR_SHUTDOWN_WAIT.getSeconds(), TimeUnit.SECONDS)) {
                        LOG.SEVERE(Strings.apply("Executor '%s' did not terminate within 60s. Interrupting "
                                                 + "tasks...", e.getKey()));
                        exec.shutdownNow();
                        if (!exec.awaitTermination(EXECUTOR_TERMINATION_WAIT.getSeconds(), TimeUnit.SECONDS)) {
                            LOG.SEVERE(Strings.apply("Executor '%s' did not terminate after another 30s!", e.getKey()));
                        }
                    }
                } catch (InterruptedException ex) {
                    Exceptions.ignore(ex);
                    LOG.SEVERE(Strings.apply("Interrupted while waiting for '%s' to terminate!", e.getKey()));
                }
            }
        }
    }

    @Override
    public String getName() {
        return "tasks (Async Execution Engine)";
    }

    @Override
    public int getPriority() {
        return LIFECYCLE_PRIORITY;
    }
}
