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
import sirius.kernel.commons.ValueHolder
import sirius.kernel.di.std.Part
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Tests the [Tasks] class.
 */
@ExtendWith(SiriusExtension::class)
class TasksTest {

    @Test
    fun `An executor executes work in the calling thread when full`() {
        // a future to synchronize the threads
        val thread2Finished: Future = Future()
        // a place to store the thread id of the thread which executes the 2nd task
        val executorThread: ValueHolder<Long> = ValueHolder.of(null)
        // we start one tasks which blocks the executor
        val task1Future: Future = tasks.executor("test-limited").start {
            thread2Finished.await(DEFAULT_TIMEOUT)
        }
        // we start another task for the blocked executor
        val task2Future: Future = tasks.executor("test-limited").start {
            executorThread.set(Thread.currentThread().id)
            thread2Finished.success()
        }
        // we wait until all background tasks are done
        task1Future.await(DEFAULT_TIMEOUT)
        task2Future.await(DEFAULT_TIMEOUT)
        // we expect the second task to be executed in the calling thread
        assertEquals(Thread.currentThread().id, executorThread.get())
    }

    @Test
    fun `An executor drops tasks (if possible) when full`() {
        // a future to synchronize the threads
        val thread2Finished: Future = Future()
        // a place to store the fact that the task was dropped
        val dropped: ValueHolder<Boolean> = ValueHolder.of(null)
        // we start one tasks which blocks the executor
        val task1Future: Future = tasks.executor("test-limited").start {
            thread2Finished.await(DEFAULT_TIMEOUT)
        }
        // we start another task for the blocked executor
        val task2Future: Future = tasks.executor("test-limited").dropOnOverload {
            dropped.set(true)
            thread2Finished.success()
        }.start {
            dropped.set(false)
            thread2Finished.success()
        }
        // we wait until all background tasks are done
        task1Future.await(DEFAULT_TIMEOUT)
        task2Future.await(DEFAULT_TIMEOUT)
        // we expect the task to be dropped
        assertTrue { dropped.get() }
        // we expect the task1 to have successfully completed
        assertTrue { task1Future.isSuccessful }
        // we expect the task1 to have failed
        assertTrue { task2Future.isFailed }
    }

    @Test
    fun `An executor uses its work queue when full`() {
        // futures to synchronize the threads
        val thread2Started: Future = Future()
        val thread2Finished: Future = Future()
        // a place to store the thread ids which executed the task
        val task1Thread: ValueHolder<Long> = ValueHolder.of(null)
        val task2Thread: ValueHolder<Long> = ValueHolder.of(null)
        // we start one tasks which blocks the executor
        tasks.executor("test-unlimited").start {
            task1Thread.set(Thread.currentThread().id)
            thread2Started.await(DEFAULT_TIMEOUT)
        }
        // we start another task for the blocked executor
        tasks.executor("test-unlimited").start {
            task2Thread.set(Thread.currentThread().id)
            thread2Finished.success()
        }
        thread2Started.success()
        // we wait until all background tasks are done
        thread2Finished.await(DEFAULT_TIMEOUT)
        // we expect both tasks to be executed in the same thread
        assertEquals(task1Thread.get(), task2Thread.get())
        // we expect both tasks not to be executed in the main thread
        assertNotEquals(Thread.currentThread().id, task1Thread.get())
    }

    @Test
    fun `An executor uses parallel threads if possible and required`() {
        // a future to synchronize the threads
        val thread2Finished: Future = Future()
        // a place to store the thread ids which executed the task
        val task1Thread: ValueHolder<Long> = ValueHolder.of(null)
        val task2Thread: ValueHolder<Long> = ValueHolder.of(null)
        // we start one tasks which blocks the executor
        tasks.executor("test-parallel").start {
            task1Thread.set(Thread.currentThread().id)
            thread2Finished.await(DEFAULT_TIMEOUT)
        }
        // we start another task for the blocked executor
        tasks.executor("test-parallel").start {
            task2Thread.set(Thread.currentThread().id)
            thread2Finished.success()
        }
        // we wait until all background tasks are done
        thread2Finished.await(DEFAULT_TIMEOUT)
        // we expect both tasks to be executed in different threads
        assertNotEquals(task1Thread.get(), task2Thread.get())
        // we expect both tasks not to be executed in the main thread
        assertNotEquals(Thread.currentThread().id, task1Thread.get())
    }

    companion object {
        val DEFAULT_TIMEOUT: Duration = Duration.ofSeconds(10)

        @Part
        @JvmStatic
        private lateinit var tasks: Tasks
    }
}
