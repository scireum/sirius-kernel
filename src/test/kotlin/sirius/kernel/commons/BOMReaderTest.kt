/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.*
import java.nio.charset.StandardCharsets

class BOMReaderTest {

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

    @Test
    @Throws(IOException::class)
    fun readBOM() {
        val outputStream = ByteArrayOutputStream()
        val writer = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
        writer.write(Streams.UNICODE_BOM_CHARACTER)
        writer.write("HELLO")
        writer.flush()
        val reader = BOMReader(InputStreamReader(ByteArrayInputStream(outputStream.toByteArray())))
        Assertions.assertEquals('H'.code.toLong(), reader.read().toLong())
        Assertions.assertEquals('E'.code.toLong(), reader.read().toLong())
    }

    @Test
    @Throws(IOException::class)
    fun readWithoutBOM() {
        val outputStream = ByteArrayOutputStream()
        val writer = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
        writer.write("HELLO")
        writer.flush()
        val reader = BOMReader(InputStreamReader(ByteArrayInputStream(outputStream.toByteArray())))
        Assertions.assertEquals('H'.code.toLong(), reader.read().toLong())
        Assertions.assertEquals('E'.code.toLong(), reader.read().toLong())
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 5])
    @Throws(IOException::class)
    fun readArrayBOM(length: Int) {
        val reader = BOMReader(InputStreamReader(ByteArrayInputStream(WITH_UTF8_BOM)))
        val buffer = CharArray(length)
        Assertions.assertEquals(length, reader.read(buffer))
        Assertions.assertEquals('H'.code.toLong(), buffer[0].code.toLong())
    }

    @Test
    @Throws(IOException::class)
    fun readArrayWithoutBOM() {
        val reader = BOMReader(InputStreamReader(ByteArrayInputStream(WITH_UTF8_BOM)))
        val buffer = CharArray(2)
        Assertions.assertEquals(2, reader.read(buffer).toLong())
        Assertions.assertEquals('H'.code.toLong(), buffer[0].code.toLong())
    }
}
