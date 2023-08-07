/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async

import org.junit.jupiter.api.Test
import sirius.kernel.commons.ValueHolder
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests the [Promise] class.
 */
class PromiseTest {

    @Test
    fun `Multiple Callbacks work on Promises when successfully completed`() {
        val test = Promise<String>();
        val resultHolder1 = ValueHolder.of("")
        val resultHolder2 = ValueHolder.of("")
        test.onSuccess(resultHolder1)
        test.onSuccess(resultHolder2)

        // successfully complete the promise
        test.success("Hello")

        // assert both handlers contain the expected result and the promise is considered successful and completed
        assertEquals(resultHolder1.get(), "Hello")
        assertEquals(resultHolder2.get(), "Hello")
        test.isSuccessful()
        test.isCompleted()
    }

    @Test
    fun `Callbacks on an already completed promise work`() {
        val test = Promise<String>()
        val resultHolder = ValueHolder.of("")
        // we successfully complete the promise and afterward add a handler
        test.success("Hello")
        test.onSuccess(resultHolder)

        assertEquals(resultHolder.get(), "Hello")
    }


    @Test
    fun `Multiple Callbacks work on Promises when failing`() {
        val test = Promise<String>()
        val exception = Exception()
        val resultHolder1: ValueHolder<Throwable> = ValueHolder.of(null)
        val resultHolder2: ValueHolder<Throwable> = ValueHolder.of(null)
        test.onFailure(resultHolder1)
        test.onFailure(resultHolder2)

        // fail the promise with our test exception
        test.fail(exception)

        // both handlers contain the expected exception and the promise is considered 'failed' and 'completed'
        assertEquals(resultHolder1.get(), exception)
        assertEquals(resultHolder2.get(), exception)
        assertTrue(test.isFailed())
        assertTrue(test.isCompleted())
    }

    @Test
    fun `Callbacks on an already failed promise work`() {
        val test = Promise<String>()
        val exception = Exception()
        val resultHolder: ValueHolder<Throwable> = ValueHolder.of(null)
        // add a dummy handler so that no output is written into the console
        test.onFailure(ValueHolder.of(null))
        // the promise fails
        test.fail(exception)
        // and after that we add a handler, it gets still invoked
        test.onFailure(resultHolder)
        assertEquals(resultHolder.get(), exception)
    }

    @Test
    fun `Mapping Promises work`() {
        val input = Promise<String>()
        // a promise which provides output based in the input promise
        val output = input.map { s -> s.uppercase() }
        val resultHolder = ValueHolder.of("")
        output.onSuccess(resultHolder)
        input.success("hello")
        // the output promise will also be completed with the computed data
        assertEquals(resultHolder.get(), "HELLO")
    }

    @Test
    fun `Exception handling when mapping Promises work`() {
        // a promise which provides output based in the input promise
        val input = Promise<String>()
        val output = input.map { s -> s.uppercase() }

        val resultHolder: ValueHolder<Throwable> = ValueHolder.of(null)
        // we attach the handler to the output promise
        output.onFailure(resultHolder)
        // complete the input with invalid data
        input.success(null)
        // the handler will contain the appropriate exception
        assertTrue(resultHolder.get() is NullPointerException)
        // the output promise is considered as 'failed'
        assertTrue(output.isFailed())
    }

    @Test
    fun `A completed Promise with a value and error is marked as non successful and failed`() {
        val test = Promise<String>()
        val exception = Exception()
        // add a dummy handler so that no output is written into the console
        test.onFailure(ValueHolder.of(null))
        // mark promise as successful
        test.success(null)
        // and mark it as failed as well
        test.fail(exception)
        // the promise is considered as 'failed' and is not considered as 'successful'
        assertTrue(test.isFailed())
        assertTrue(!test.isSuccessful())
    }
}
