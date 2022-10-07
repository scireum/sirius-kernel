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
import java.io.IOException
import java.io.InputStreamReader
import kotlin.test.assertEquals

class BOMReaderTest {

    companion object {
        private val WITH_UTF8_BOM = byteArrayOf(
            239.toByte(),
            187.toByte(),
            191.toByte(),
            'H'.toByte(),
            'E'.toByte(),
            'L'.toByte(),
            'L'.toByte(),
            'O'.toByte()
        )
        private val WITHOUT_BOM = byteArrayOf('H'.toByte(), 'E'.toByte(), 'L'.toByte(), 'L'.toByte(), 'O'.toByte())
    }

    @Test
    @Throws(IOException::class)
    fun readBOM() {
        val reader = BOMReader(InputStreamReader(ByteArrayInputStream(WITH_UTF8_BOM)))
        assertEquals('H'.code.toLong(), reader.read().toLong())
        assertEquals('E'.code.toLong(), reader.read().toLong())
    }

    @Test
    @Throws(IOException::class)
    fun readWithoutBOM() {
        val reader = BOMReader(InputStreamReader(ByteArrayInputStream(WITHOUT_BOM)))
        assertEquals('H'.code.toLong(), reader.read().toLong())
        assertEquals('E'.code.toLong(), reader.read().toLong())
    }

    @Test
    @Throws(IOException::class)
    fun readArray1BOM() {
        val reader = BOMReader(InputStreamReader(ByteArrayInputStream(WITH_UTF8_BOM)))
        val buffer = CharArray(1)
        assertEquals(1, reader.read(buffer).toLong())
        assertEquals('H'.code.toLong(), buffer[0].code.toLong())
    }

    @Test
    @Throws(IOException::class)
    fun readArray2BOM() {
        val reader = BOMReader(InputStreamReader(ByteArrayInputStream(WITH_UTF8_BOM)))
        val buffer = CharArray(2)
        assertEquals(2, reader.read(buffer).toLong())
        assertEquals('H'.code.toLong(), buffer[0].code.toLong())
    }

    @Test
    @Throws(IOException::class)
    fun readArray10BOM() {
        val reader = BOMReader(InputStreamReader(ByteArrayInputStream(WITH_UTF8_BOM)))
        val buffer = CharArray(10)
        assertEquals(5, reader.read(buffer).toLong())
        assertEquals('H'.code.toLong(), buffer[0].code.toLong())
    }

    @Test
    @Throws(IOException::class)
    fun readArrayWithoutBOM() {
        val reader = BOMReader(InputStreamReader(ByteArrayInputStream(WITH_UTF8_BOM)))
        val buffer = CharArray(2)
        assertEquals(2, reader.read(buffer).toLong())
        assertEquals('H'.code.toLong(), buffer[0].code.toLong())
    }
}
