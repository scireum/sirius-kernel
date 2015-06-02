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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
public class Async {

    /**
     * Contains the name of the default executor.
     */
    public static final String DEFAULT = "default";

    /**
     * Contains the priority of this {@link Lifecycle}
     */
    public static final int LIFECYCLE_PRIORITY = 25;

    protected static final Log LOG = Log.get("async");
    protected static final Map<String, AsyncExecutor> executors = Maps.newConcurrentMap();
    protected static List<BackgroundQueueWorker> backgroundWorkers = Lists.newArrayList();

    // If sirius is not started yet, we still consider it running already as the intention of this flag
    // is to detect a system halt and not to check if the startup sequence has finished.
    private static volatile boolean running = true;

    @Parts(BackgroundTaskQueue.class)
    private static PartCollection<BackgroundTaskQueue> backgroundQueues;

    private Async() {
    }

    /**
     * Returns the executor for the given category.
     * <p>
     * The configuration for this executor is taken from <tt>async.executor.[category]</tt>. If no config is found,
     * the default values are used.
     *
     * @param category the category of the task to be executed, which implies the executor to use.
     * @return the execution builder which submits tasks to the appropriate executor.
     */
    public static ExecutionBuilder executor(String category) {
        return new ExecutionBuilder(category);
    }

    /**
     * Returns the default executor.
     *
     * @return the execution builder which submits tasks to the default executor.
     */
    public static ExecutionBuilder defaultExecutor() {
        return new ExecutionBuilder(DEFAULT);
    }

    /*
     * Executes a given TaskWrapper by fetching or creating the appropriate executor and submitting the wrapper.
     */
    protected static void execute(ExecutionBuilder.TaskWrapper wrapper) {
        wrapper.prepare();
        AsyncExecutor exec = executors.get(wrapper.category);
        if (exec == null) {
            synchronized (executors) {
                exec = executors.get(wrapper.category);
                if (exec == null) {
                    Extension config = Extensions.getExtension("async.executor", wrapper.category);
                    exec = new AsyncExecutor(wrapper.category,
                                             config.get("poolSize").getInteger(),
                                             config.get("queueLength").getInteger());
                    executors.put(wrapper.category, exec);
                }
            }
        }
        wrapper.jobNumber = exec.executed.inc();
        wrapper.durationAverage = exec.duration;
        exec.execute(wrapper);
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
    public static <V> Promise<V> fork(String category, final Supplier<V> computation) {
        final Promise<V> result = promise();

        executor(category).fork(() -> {
            try {
                result.success(computation.get());
            } catch (Throwable t) {
                result.fail(t);
            }
        }).dropOnOverload(() -> result.fail(new RejectedExecutionException())).execute();

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
    public static Collection<AsyncExecutor> getExecutors() {
        return Collections.unmodifiableCollection(executors.values());
    }

    /**
     * Returns a list of all background workers
     *
     * @return a list of the state of all background workers
     */
    public static Collection<String> getBackgroundWorkers() {
        return backgroundWorkers.stream().map(Object::toString).collect(Collectors.toList());
    }

    /**
     * Determines if the application is still running.
     * <p>
     * Can be used for long loops in async tasks to determine if a computation should be interrupted.
     *
     * @return <tt>true</tt> if the system is running, <tt>false</tt> if a shutdown is in progress
     */
    public static boolean isRunning() {
        return running;
    }

    /**
     * Ensures that all thread pools are halted, when the system shuts down.
     */
    @Register
    public static class AsyncLifecycle implements Lifecycle {

        @Override
        public void started() {
            running = true;
            for (BackgroundTaskQueue tq : backgroundQueues) {
                backgroundWorkers.add(new BackgroundQueueWorker(tq).start());
            }
        }

        @SuppressWarnings("Convert2streamapi")
        @Override
        public void stopped() {
            running = false;
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
                                LOG.SEVERE(Strings.apply("Executor '%s' did not terminate after another 30s!",
                                                         e.getKey()));
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
            return "async (Async Execution Engine)";
        }

        @Override
        public int getPriority() {
            return LIFECYCLE_PRIORITY;
        }
    }
}
