/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons

import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals

/**
 * Tests the [Streams] class.
 */
class StreamsTest {

    @Test
    fun transferTest() {
        val testString = "Hello from the other side..."
        val source = ByteArrayInputStream(testString.toByteArray(StandardCharsets.UTF_8))
        val target = ByteArrayOutputStream()
        Streams.transfer(source, target)
        assertEquals(testString, String(target.toByteArray(), StandardCharsets.UTF_8))
    }

    @Test
    fun largeTransferTest() {
        val builder = StringBuilder()
        builder.append("Hello World".repeat(10000))
        val testString = builder.toString()
        val source = ByteArrayInputStream(testString.toByteArray(StandardCharsets.UTF_8))
        val target = ByteArrayOutputStream()
        Streams.transfer(source, target)
        assertEquals(testString, String(target.toByteArray(), StandardCharsets.UTF_8))
    }
}
