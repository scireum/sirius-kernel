/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import sirius.kernel.SiriusExtension
import java.time.Duration
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the [ParallelTaskExecutor] class.
 */
@ExtendWith(SiriusExtension::class)
class ParallelTaskExecutorTest {

    @Test
    fun `submitTask executes a single submitted task`() {
        val executor = ParallelTaskExecutor(2)
        val taskFinished = Future()

        val isSubmitted = executor.submitTask {
            taskFinished.success()
        }

        assertTrue { isSubmitted }
        taskFinished.await(DEFAULT_TIMEOUT)
        assertTrue { taskFinished.isSuccessful }

        executor.shutdownWhenDone()
        assertEquals(0, executor.taskCount)
    }

    @Test
    fun `submitTask runs tasks up to the configured concurrency limit in parallel`() {
        val maxConcurrent = 2
        val executor = ParallelTaskExecutor(maxConcurrent)
        // the first `maxConcurrent` tasks rendezvous on this barrier to deterministically
        // prove that the executor actually runs them in parallel
        val parallelismBarrier = CyclicBarrier(maxConcurrent)
        val maxConcurrentObserved = AtomicInteger(0)
        val currentlyRunning = AtomicInteger(0)
        val executedTasks = AtomicInteger(0)
        val totalTasks = 10

        repeat(totalTasks) { index ->
            executor.submitTask {
                val running = currentlyRunning.incrementAndGet()
                maxConcurrentObserved.updateAndGet { current -> maxOf(current, running) }
                if (index < maxConcurrent) {
                    parallelismBarrier.await(DEFAULT_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
                }
                currentlyRunning.decrementAndGet()
                executedTasks.incrementAndGet()
            }
        }

        executor.shutdownWhenDone()

        assertEquals(totalTasks, executedTasks.get())
        assertEquals(0, executor.taskCount)
        assertEquals(maxConcurrent, maxConcurrentObserved.get())
    }

    @Test
    fun `shutdownWhenDone returns when isActiveSupplier becomes false while tasks are still pending`() {
        val executor = ParallelTaskExecutor(1)
        val active = java.util.concurrent.atomic.AtomicBoolean(true)
        executor.withIsActiveSupplier { active.get() }

        // submit a task that blocks indefinitely, keeping taskCount > 0
        val taskStarted = Future()
        val releaseTask = Future()
        executor.submitTask {
            taskStarted.success()
            releaseTask.await(DEFAULT_TIMEOUT)
        }
        // make sure the task is actually running so taskCount stays > 0 throughout
        taskStarted.await(DEFAULT_TIMEOUT)

        // call shutdownWhenDone on a separate thread; with active=true and a pending
        // task it must keep waiting and not return on its own
        val shutdownCompleted = Future()
        Thread.startVirtualThread {
            executor.shutdownWhenDone()
            shutdownCompleted.success()
        }

        // give it ample time to (incorrectly) return; the wait loop polls every 200ms
        Thread.sleep(600)
        assertFalse(
            "shutdownWhenDone returned despite a pending task and active supplier still being true"
        ) { shutdownCompleted.isCompleted }

        // flipping the supplier must let the wait loop exit on the next poll
        active.set(false)
        // unblock the running task so executor.close() can finish terminating
        releaseTask.success()

        shutdownCompleted.await(DEFAULT_TIMEOUT)
        assertTrue { shutdownCompleted.isSuccessful }
        assertFalse { executor.isActive }
    }

    companion object {
        val DEFAULT_TIMEOUT: Duration = Duration.ofSeconds(10)
    }
}
