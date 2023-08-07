/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons

import org.junit.jupiter.api.Test
import java.util.stream.Stream
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests the [Limit] class.
 */
class LimitTest {

    @Test
    fun testSingleItem() {
        executeLimit(Limit.singleItem(), 1, listOf(1))
    }

    @Test
    fun testUnlimited() {
        executeLimit(Limit.UNLIMITED, 10, listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
    }

    @Test
    fun testSkip() {
        executeLimit(Limit(5, 0), 10, listOf(6, 7, 8, 9, 10))
    }

    @Test
    fun testLimit() {
        executeLimit(Limit(0, 5), 5, listOf(1, 2, 3, 4, 5))
    }

    @Test
    fun testSkipAndLimit() {
        executeLimit(Limit(2, 5), 7, listOf(3, 4, 5, 6, 7))
    }

    @Test
    fun testBadValues() {
        // Negative start and negative limit are ignored
        executeLimit(Limit(-1, -5), 10, listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
    }

    @Test
    fun testNullLimit() {
        // Null as limit is unlimited
        executeLimit(Limit(0, null), 10, listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
    }

    @Test
    fun testPredicate() {
        // Use as predicate to filter a sublist of the given stream to compute a test sum
        val sum = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).filter(Limit(2, 4).asPredicate()).mapToInt { it }.sum()
        assertEquals(3 + 4 + 5 + 6, sum)
    }

    @Test
    fun testRemainingItems() {
        Limit(5, 500).apply { ->
            assertEquals(505, this.remainingItems)
        }
        Limit(10, 0).apply {
            assertNull(this.remainingItems)
        }
        Limit(10, 10).apply {
            (0 until 12).forEach { _ -> this.nextRow() }
            assertEquals(8, this.remainingItems)
        }
        Limit(10, 10).apply {
            (0 until 21).forEach { _ -> this.nextRow() }
            assertEquals(0, this.remainingItems)
        }
    }

    private fun executeLimit(limit: Limit, expectedIterations: Int, expected: List<Int>) {
        val result = mutableListOf<Int>()
        var iterations = 0
        for (i in 1..10) {
            iterations++
            if (limit.nextRow()) {
                result.add(i)
            }
            if (!limit.shouldContinue()) {
                break
            }
        }
        assertContentEquals(expected, result, "Expected result not met for: $limit")
        assertEquals(expectedIterations, iterations, "Expected iterations not met for: $limit")
    }
}
