package sirius.kernel.commons

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PriorityCollectorTest {
    @Test
    fun test() {
        val c = PriorityCollector.create<String>()
        c.addDefault("B")
        c.add(50, "A")
        c.add(101, "C")
        assertEquals(listOf("A", "B", "C"), c.getData())
        c.getData().clear()
        assertEquals(3, c.size().toLong())
    }
}
