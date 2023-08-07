/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import sirius.kernel.SiriusExtension
import java.io.StringReader
import java.io.StringWriter
import kotlin.test.assertEquals

@ExtendWith(SiriusExtension::class)
class CSVWriterTest {

    @Test
    fun `simple data is output as CSV`() {
        StringWriter().use { output ->
            val writer = CSVWriter(output)

            writer.writeArray("a", "b", "c")
            writer.writeArray(1, 2, 3)
            writer.writeList(listOf("d", "e", "f"))

            assertEquals("a;b;c\n1;2;3\nd;e;f", output.toString())
        }
    }

    @Test
    fun `data with BOM can be written and re-read`() {
        StringWriter().use { output ->
            val writer = CSVWriter(output)

            writer.writeUnicodeBOM()
            writer.writeArray("a", "b", "c")
            writer.writeArray(1, 2, 3)
            writer.writeList(listOf("d", "e", "f"))

            val csvData = output.toString()
            var row: Values? = null
            CSVReader(BOMReader(StringReader(csvData))).execute { data ->
                if (row == null) {
                    row = data
                }
            }

            assertEquals(Streams.UNICODE_BOM_CHARACTER, csvData[0].code)
            assertEquals("a", row?.at(0)?.asString())
        }
    }

    @Test
    fun `changing the lineSeparator works`() {
        StringWriter().use { output ->
            val writer = CSVWriter(output).withLineSeparator("\r\n")

            writer.writeArray("a", "b", "c")
            writer.writeArray(1, 2, 3)
            writer.writeList(listOf("d", "e", "f"))

            assertEquals("a;b;c\r\n1;2;3\r\nd;e;f", output.toString())
        }
    }

    @Test
    fun `quotation works for separator and new line`() {
        StringWriter().use { output ->
            val writer = CSVWriter(output)

            writer.writeArray("a;", "b", "c")
            writer.writeArray("1", "2\n2", "3")

            assertEquals("\"a;\";b;c\n1;\"2\n2\";3", output.toString())
        }
    }

    @Test
    fun `escaping of quotation works when using quotation`() {
        StringWriter().use { output ->
            val writer = CSVWriter(output)

            writer.writeArray("\"a\"\nb")
            writer.writeArray("\"a\";b")

            assertEquals("\"\\\"a\\\"\nb\"\n\"\\\"a\\\";b\"", output.toString())
        }
    }

    @Test
    fun `escaping works for escape character and quotation`() {
        StringWriter().use { output ->
            val writer = CSVWriter(output)

            writer.writeArray("a;b\"", "\\", "c")

            assertEquals("\"a;b\\\"\";\\\\;c", output.toString())
        }
    }

    @Test
    fun `escaping of separator with escape-char works if there is no quotation-char`() {
        StringWriter().use { output ->
            val writer = CSVWriter(output).withQuotation(0.toChar())

            writer.writeArray("a;b")

            assertEquals("a\\;b", output.toString())
        }
    }

    @Test
    fun `throw an exception if we have to escape quotes, but there is no escape-char`() {
        StringWriter().use { output ->
            val writer = CSVWriter(output).withEscape(0.toChar())

            val exception = assertThrows<java.lang.IllegalArgumentException> {
                writer.writeArray("\"a\";b")
            }
            assertEquals(
                    "Cannot output a quotation character within a quoted string without an escape character.",
                    exception.message
            )
        }
    }

    @Test
    fun `throw an exception if there is a separator in the text, but there is no quotation-char and no escape-char`() {
        StringWriter().use { output ->
            val writer = CSVWriter(output).withQuotation(0.toChar()).withEscape(0.toChar())

            val exception = assertThrows<java.lang.IllegalArgumentException> {
                writer.writeArray("'a;b")
            }
            assertEquals(
                    "Cannot output a column which contains the separator character ';' without an escape or quotation character.",
                    exception.message
            )
        }
    }

    @Test
    fun `throw an exception if there is a new line in the text, but there is no quotation-char`() {
        StringWriter().use { output ->
            val writer = CSVWriter(output).withQuotation(0.toChar())

            val exception = assertThrows<java.lang.IllegalArgumentException> {
                writer.writeArray("a\nb")
            }
            assertEquals(
                    "Cannot output a column which contains a line break without an quotation character.",
                    exception.message
            )
        }
    }
}
