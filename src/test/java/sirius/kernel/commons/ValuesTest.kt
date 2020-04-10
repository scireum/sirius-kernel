/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */
package sirius.kernel.commons

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ValuesTest {
    @Test
    fun at() {
        assertEquals("A", Values.of(arrayOf("A", "B", "C")).at(0).asString())
        assertFalse(Values.of(arrayOf("A", "B", "C")).at(10).isFilled)
    }

    @Test
    fun excelStyleColumns() {
        assertEquals("A", Values.of(arrayOf("A", "B", "C")).at("A").asString())
        assertEquals("C", Values.of(arrayOf("A", "B", "C")).at("C").asString())
        val test: MutableList<String?> = mutableListOf()
        for (i in 1..99) {
            test.add(i.toString())
        }
        assertEquals("28", Values.of(test).at("AB").asString())
        assertEquals("34", Values.of(test).at("AH").asString())
    }
}
