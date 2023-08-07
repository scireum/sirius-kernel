/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import sirius.kernel.SiriusExtension
import java.io.StringReader
import kotlin.test.assertEquals

/**
 * Tests the [CSVReader] class.
 */
@ExtendWith(SiriusExtension::class)
class CSVReaderTest {

    @Test
    fun `valid CSV data can be parsed`() {
        val data = "a;b;c\n1;2;3\r\n4;5;6"
        val output = mutableListOf<Values>()
        CSVReader(StringReader(data)).execute { output.add(it) }

        assertEquals(3, output.size)
        assertEquals("a", output[0].at("A").rawString)
        assertEquals("b", output[0].at("B").rawString)
        assertEquals("c", output[0].at("C").rawString)
        assertEquals("1", output[1].at("A").rawString)
        assertEquals("2", output[1].at("B").rawString)
        assertEquals("3", output[1].at("C").rawString)
        assertEquals("4", output[2].at("A").rawString)
        assertEquals("5", output[2].at("B").rawString)
        assertEquals("6", output[2].at("C").rawString)

    }

    @Test
    fun `empty cells become an empty string`() {
        val data = "a;;c"
        val output = mutableListOf<Values>()
        CSVReader(StringReader(data)).execute { output.add(it) }

        assertEquals(1, output.size)
        assertEquals("a", output[0].at("A").rawString)
        assertEquals("", output[0].at("B").rawString)
        assertEquals("c", output[0].at("C").rawString)
    }

    @Test
    fun `quotation is detected and handled correctly`() {
        val data = """
            "a";"b;";1/4";d
        """.trimIndent()
        val output = mutableListOf<Values>()
        CSVReader(StringReader(data)).execute { output.add(it) }

        assertEquals(1, output.size)
        assertEquals("a", output[0].at("A").rawString)
        assertEquals("b;", output[0].at("B").rawString)
        assertEquals("1/4\"", output[0].at("C").rawString)
        assertEquals("d", output[0].at("D").rawString)
    }

    @Test
    fun `escaping works`() {
        val data = """
            \"a;\;;\\;x
        """.trimIndent()
        val output = mutableListOf<Values>()
        CSVReader(StringReader(data)).execute { output.add(it) }

        assertEquals(1, output.size)
        assertEquals("\"a", output[0].at("A").rawString)
        assertEquals(";", output[0].at("B").rawString)
        assertEquals("\\", output[0].at("C").rawString)
        assertEquals("x", output[0].at("D").rawString)
    }

    @Test
    fun `empty cells work with and without quotation`() {
        val data = """
            a;;"";d
        """.trimIndent()
        val output = mutableListOf<Values>()
        CSVReader(StringReader(data)).execute { output.add(it) }

        assertEquals(1, output.size)
        assertEquals("a", output[0].at("A").rawString)
        assertEquals("", output[0].at("B").rawString)
        assertEquals("", output[0].at("C").rawString)
        assertEquals("d", output[0].at("D").rawString)
    }

    @Test
    fun `multiline values work`() {
        val data = "\"a\nb\";\"c\r\nd\";e\nf"
        val output = mutableListOf<Values>()
        CSVReader(StringReader(data)).execute { output.add(it) }

        assertEquals(2, output.size)
        assertEquals("a\nb", output[0].at("A").rawString)
        assertEquals("c\r\nd", output[0].at("B").rawString)
        assertEquals("e", output[0].at("C").rawString)
        assertEquals("f", output[1].at("A").rawString)
    }

    @Test
    fun `ignoring whitespaces works`() {
        val data = "  a  ; \"b\" ;\t\"c\"\t; \" \td\t \";\te\t;f "
        val output = mutableListOf<Values>()
        CSVReader(StringReader(data)).execute { output.add(it) }

        assertEquals(1, output.size)
        assertEquals("  a  ", output[0].at("A").rawString)
        assertEquals("b", output[0].at("B").rawString)
        assertEquals("c", output[0].at("C").rawString)
        assertEquals(" \td\t ", output[0].at("D").rawString)
        assertEquals("\te\t", output[0].at("E").rawString)
        assertEquals("f ", output[0].at("F").rawString)
    }

    @Test
    fun `modified settings work`() {
        val data = "!a! : !b! :&:c"
        val output = mutableListOf<Values>()

        CSVReader(StringReader(data)).withSeparator(':').withQuotation('!').withEscape('&').notIgnoringWhitespaces()
                .execute { output.add(it) }

        assertEquals(1, output.size)
        assertEquals("a", output[0].at("A").rawString)
        assertEquals(" !b! ", output[0].at("B").rawString)
        assertEquals(":c", output[0].at("C").rawString)
    }

    @Test
    fun `quoted strings treat double quotes as escaped quote`() {
        val data = """
            "test""X""${'"'};"a";b""
        """.trimIndent()
        val output = mutableListOf<Values>()
        CSVReader(StringReader(data)).execute { output.add(it) }

        assertEquals(1, output.size)
        assertEquals("test\"X\"", output[0].at("A").rawString)
        assertEquals("a", output[0].at("B").rawString)
        assertEquals("b\"\"", output[0].at("C").rawString)
    }

    @Test
    fun `limit the number of line to read`() {
        val completeData = (0..99).joinToString("\n") { "a;b;c\n1;2;3\r\n4;5;6" }
        val output = mutableListOf<Values>()
        CSVReader(StringReader(completeData)).withLimit(Limit(0, 100)).execute { output.add(it) }

        assertEquals(100, output.size)
    }

    @Test
    fun `skip and limit the number of line to read`() {
        val completeData = (0..99).joinToString("\n") { "a;b;c\n1;2;3\r\n4;5;6" }

        val output = mutableListOf<Values>()
        CSVReader(StringReader(completeData)).withLimit(Limit(250, 100))
                .execute { output.add(it) }

        assertEquals(50, output.size)
    }
}
