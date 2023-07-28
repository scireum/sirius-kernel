/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the [CombinedFuture] class.
 */
class CombinedFutureTest {

    @Test
    fun emptyCombinedFutureDontBlock() {
        val combinedFuture = CombinedFuture()
        assertTrue { combinedFuture.asFuture().isCompleted }
    }

    @Test
    fun combinedFutureActuallyAwait() {
        val combinedFuture = CombinedFuture()
        val future = Future()
        combinedFuture.add(future)
        assertFalse { combinedFuture.asFuture().isCompleted }
        future.success()
        assertTrue { combinedFuture.asFuture().isCompleted }
    }

    @Test
    fun combinedFutureWorkWithCompletedFutures() {
        val combinedFuture = CombinedFuture()
        val future = Future().success()
        combinedFuture.add(future)
        assertTrue { combinedFuture.asFuture().isCompleted }
    }

    @Test
    fun combinedFutureWorkWithFailingFutures() {
        val combinedFuture = CombinedFuture()
        val future = Future()
        combinedFuture.add(future)
        future.fail(IllegalStateException("ThisIsExpected"))
        assertTrue { combinedFuture.asFuture().isCompleted }
    }
}
