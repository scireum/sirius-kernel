/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

import sirius.kernel.commons.Wait;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Executes tasks in parallel in the current node using virtual threads with a limit on concurrency.
 * <p>
 * After the executor has been initialized, submit tasks using {@link #submitTask(Runnable)}. They will start as
 * soon as the first task is submitted. The underlying queue is unbounded and only limited by the memory available.
 * Once all tasks have been submitted, call {@link #shutdownWhenDone()} to wait for all pending tasks to complete.
 * <p>
 * Note that the {@linkplain CallContext#getCurrent() current context} will be passed to the tasks when they are
 * executed.
 */
public class ParallelTaskExecutor {

    private final ExecutorService executor;
    private final BlockingQueue<Runnable> taskQueue;
    private final Semaphore semaphore;
    private final AtomicInteger taskCount;
    private final CallContext currentContext;

    /**
     * Creates a new parallel task executor.
     *
     * @param maxConcurrentTasks the maximum number of tasks to run concurrently
     */
    public ParallelTaskExecutor(int maxConcurrentTasks) {
        this.currentContext = CallContext.getCurrent();
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.taskQueue = new LinkedBlockingQueue<>();
        this.semaphore = new Semaphore(maxConcurrentTasks);
        this.taskCount = new AtomicInteger(0);
        startProcessing();
    }

    /**
     * Submits a task to be executed in parallel.
     *
     * @param task the task to execute
     * @return {@code true} if the task was successfully submitted, {@code false} otherwise
     */
    public boolean submitTask(Runnable task) {
        return taskQueue.offer(() -> {
            try {
                CallContext.setCurrent(currentContext);
                taskCount.incrementAndGet();
                task.run();
            } finally {
                taskCount.decrementAndGet();
                semaphore.release();
            }
        });
    }

    /**
     * Waits for all tasks to complete and shuts down the executor.
     */
    public void shutdownWhenDone() {
        while (TaskContext.get().isActive()) {
            if (taskQueue.isEmpty() && taskCount.get() == 0) {
                break;
            }
            Wait.millis(500);
        }
        executor.close();
    }

    private void startProcessing() {
        Thread.startVirtualThread(() -> {
            while (TaskContext.get().isActive()) {
                try {
                    Runnable task = taskQueue.take();
                    semaphore.acquire();
                    executor.submit(task);
                } catch (InterruptedException _) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }
}
