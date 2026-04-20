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
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
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
    fun `submitTask executes multiple tasks respecting the concurrency limit`() {
        val executor = ParallelTaskExecutor(2)
        val maxConcurrentObserved = AtomicInteger(0)
        val currentlyRunning = AtomicInteger(0)
        val executedTasks = AtomicInteger(0)
        val totalTasks = 10

        repeat(totalTasks) {
            executor.submitTask {
                val running = currentlyRunning.incrementAndGet()
                maxConcurrentObserved.updateAndGet { current -> maxOf(current, running) }
                // small wait to make overlap visible
                Thread.sleep(50)
                currentlyRunning.decrementAndGet()
                executedTasks.incrementAndGet()
            }
        }

        executor.shutdownWhenDone()

        assertEquals(totalTasks, executedTasks.get())
        assertEquals(0, executor.taskCount)
        assertTrue("Expected at most 2 concurrent tasks but observed ${maxConcurrentObserved.get()}") {
            maxConcurrentObserved.get() <= 2
        }
    }

    @Test
    fun `shutdownWhenDone returns when isActiveSupplier becomes false`() {
        val executor = ParallelTaskExecutor(1)
        val active = java.util.concurrent.atomic.AtomicBoolean(true)
        executor.withIsActiveSupplier { active.get() }

        val taskFinished = Future()
        executor.submitTask {
            taskFinished.success()
        }
        taskFinished.await(DEFAULT_TIMEOUT)

        // flipping the supplier to false must allow shutdownWhenDone to return promptly
        active.set(false)
        executor.shutdownWhenDone()

        assertTrue { !executor.isActive }
    }

    companion object {
        val DEFAULT_TIMEOUT: Duration = Duration.ofSeconds(10)
    }
}
