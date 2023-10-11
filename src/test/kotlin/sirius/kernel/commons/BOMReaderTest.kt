/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.*
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals

/**
 * Tests the [BOMReader] class.
 */
class BOMReaderTest {
    @Test
    fun readBOM() {
        val outputStream = ByteArrayOutputStream()
        val writer: Writer = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
        writer.write(Streams.UNICODE_BOM_CHARACTER)
        writer.write("HELLO")
        writer.flush()
        val reader = BOMReader(InputStreamReader(ByteArrayInputStream(outputStream.toByteArray())))
        assertEquals('H'.code, reader.read())
        assertEquals('E'.code, reader.read())
    }

    @Test
    fun readWithoutBOM() {
        val outputStream = ByteArrayOutputStream()
        val writer: Writer = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
        writer.write("HELLO")
        writer.flush()
        val reader = BOMReader(InputStreamReader(ByteArrayInputStream(outputStream.toByteArray())))
        assertEquals('H'.code, reader.read())
        assertEquals('E'.code, reader.read())
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 5])
    fun readArrayBOM(length: Int) {
        val reader = BOMReader(InputStreamReader(ByteArrayInputStream(WITH_UTF8_BOM)))
        val buffer = CharArray(length)
        assertEquals(length, reader.read(buffer))
        assertEquals('H', buffer[0])
    }

    @Test
    fun readArrayWithoutBOM() {
        val reader = BOMReader(InputStreamReader(ByteArrayInputStream(WITH_UTF8_BOM)))
        val buffer = CharArray(2)
        assertEquals(2, reader.read(buffer))
        assertEquals('H', buffer[0])
    }

    companion object {
        private val WITH_UTF8_BOM = byteArrayOf(
                239.toByte(),
                187.toByte(),
                191.toByte(),
                'H'.code.toByte(),
                'E'.code.toByte(),
                'L'.code.toByte(),
                'L'.code.toByte(),
                'O'.code.toByte()
        )
    }
}
