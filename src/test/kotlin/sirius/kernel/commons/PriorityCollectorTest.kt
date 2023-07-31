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

/**
 * Tests the [PriorityCollector] class.
 */
class PriorityCollectorTest {

    @Test
    fun test() {
        val collector = PriorityCollector.create<String>()
        collector.addDefault("B")
        collector.add(50, "A")
        collector.add(101, "C")

        assertEquals(mutableListOf("A", "B", "C"), collector.getData())

        collector.getData().clear()

        assertEquals(3, collector.size())
    }
}
