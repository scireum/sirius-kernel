/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons

import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals

/**
 * Tests the [MultiMap] class.
 */
class MultiMapTest {

    @Test
    fun test() {
        val underTest = MultiMap.create<String, String>()
        underTest.put("A", "A")
        underTest.put("A", "B")
        assertContentEquals(listOf("A", "B"), underTest["A"])
        underTest.put("A", "B")
        assertContentEquals(listOf("A", "B", "B"), underTest["A"])
        underTest.remove("A", "B")
        assertContentEquals(listOf("A"), underTest["A"])
        underTest.put("B", "A")
        underTest.put("B", "C")
        assertContentEquals(listOf("A", "B"), underTest.keySet())
        assertContentEquals(listOf("A", "A", "C"), underTest.values())
        underTest.underlyingMap.clear()
        assertContentEquals(listOf(), underTest["A"])
        underTest.put("B", "C")
        assertContentEquals(listOf("C"), underTest["B"])
        underTest.clear()
        assertContentEquals(listOf(), underTest["B"])
    }
}
