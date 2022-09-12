/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async

import sirius.kernel.BaseSpecification
import sirius.kernel.commons.ValueHolder
import sirius.kernel.di.std.Part

import java.time.Duration

class TasksSpec extends BaseSpecification {

    @Part
    public static Tasks tasks

    static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10)

    def "An executor executes work in the calling thread when full"() {
        given: "a future the synchronize the threads"
        Future thread2Finished = new Future()
        and: "a place to store the thread id of the thread which executes the 2nd task"
        ValueHolder<Long> executorThread = ValueHolder.of(null)
        when: "we start one tasks which blocks the executor"
        Future task1Future = tasks.executor("test-limited").start({ thread2Finished.await(DEFAULT_TIMEOUT) })
        and: "we start another task for the blocked executor"
        Future task2Future = tasks.executor("test-limited").start({
            executorThread.set(Thread.currentThread().getId())
            thread2Finished.success()
        })
        and: "we wait until all background tasks are done"
        task1Future.await(DEFAULT_TIMEOUT)
        task2Future.await(DEFAULT_TIMEOUT)
        then: "we expect the second task to be executed in the calling thread"
        Thread.currentThread().getId() == executorThread.get()
    }

    def "An executor drops tasks (if possible) when full"() {
        given: "a future the synchronize the threads"
        Future thread2Finished = new Future()
        and: "a place to store the fact that the task was dropped"
        ValueHolder<Boolean> dropped = ValueHolder.of(null)
        when: "we start one tasks which blocks the executor"
        Future task1Future = tasks.executor("test-limited").start({
            thread2Finished.await(DEFAULT_TIMEOUT)
        })
        and: "we start another task for the blocked executor"
        Future task2Future = tasks.executor("test-limited").dropOnOverload({
            dropped.set(true)
            thread2Finished.success()
        }).start({
            dropped.set(false)
            thread2Finished.success()
        })
        and: "we wait until all background tasks are done"
        task1Future.await(DEFAULT_TIMEOUT)
        task2Future.await(DEFAULT_TIMEOUT)
        then: "we expect the task the be dropped"
        dropped.get() == true
        and: "we expect the task1 to have successfully completed"
        task1Future.isSuccessful()
        and: "we expect the task1 to have failed"
        task2Future.isFailed()
    }

    def "An executor uses its work queue when full"() {
        given: "futures the synchronize the threads"
        Future thread2Started = new Future()
        Future thread2Finished = new Future()
        and: "a place to store the thread ids which executed the task"
        ValueHolder<Long> task1Thread = ValueHolder.of(null)
        ValueHolder<Long> task2Thread = ValueHolder.of(null)
        when: "we start one tasks which blocks the executor"
        tasks.executor("test-unlimited").start({
            task1Thread.set(Thread.currentThread().getId())
            thread2Started.await(DEFAULT_TIMEOUT)
        })
        and: "we start another task for the blocked executor"
        tasks.executor("test-unlimited").start({
            task2Thread.set(Thread.currentThread().getId())
            thread2Finished.success()
        })
        thread2Started.success()
        and: "we wait until all background tasks are done"
        thread2Finished.await(DEFAULT_TIMEOUT)
        then: "we expect both tasks to be executed in the same thread"
        task1Thread.get() == task2Thread.get()
        then: "we expect both tasks not to be executed in the main thread"
        Thread.currentThread().getId() != task1Thread.get()
    }

    def "An executor uses parallel threads if possible and required"() {
        given: "a future the synchronize the threads"
        Future thread2Finished = new Future()
        and: "a place to store the thread ids which executed the task"
        ValueHolder<Long> task1Thread = ValueHolder.of(null)
        ValueHolder<Long> task2Thread = ValueHolder.of(null)
        when: "we start one tasks which blocks the executor"
        tasks.executor("test-parallel").start({
            task1Thread.set(Thread.currentThread().getId())
            thread2Finished.await(DEFAULT_TIMEOUT)
        })
        and: "we start another task for the blocked executor"
        tasks.executor("test-parallel").start({
            task2Thread.set(Thread.currentThread().getId())
            thread2Finished.success()
        })
        and: "we wait until all background tasks are done"
        thread2Finished.await(DEFAULT_TIMEOUT)
        then: "we expect both tasks to be executed in different threads"
        task1Thread.get() != task2Thread.get()
        then: "we expect both tasks not to be executed in the main thread"
        Thread.currentThread().getId() != task1Thread.get()
    }

}
