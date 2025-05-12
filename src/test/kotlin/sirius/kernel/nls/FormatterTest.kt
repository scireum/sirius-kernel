/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.nls

import org.junit.Assert.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import sirius.kernel.SiriusExtension
import sirius.kernel.commons.Context
import kotlin.test.assertEquals

/**
 * Tests the [Formatter] class.
 */
@ExtendWith(SiriusExtension::class)
class FormatterTest {

    @Test
    fun `format replaces parameters`() {
        val pattern = "Test \${foo}"

        val result = Formatter.create(pattern).set("foo", "bar").format()

        assertEquals("Test TODO REMOVE bar", result)
    }

    @Test
    fun `set trims and calls toUserString on parameters`() {
        val pattern = "Test \${foo} \${bar}"

        val result = Formatter.create(pattern).set("foo", true).set("bar", " test ").format()

        assertEquals("Test Ja test", result)
    }

    @Test
    fun `setDirect neither trims nor calls toUserString on parameters`() {
        val pattern = "Test \${foo} \${bar}"

        val result = Formatter.create(pattern).setDirect(Context.create().set("foo", true)).setDirect("bar", " test ")
                .format()

        assertEquals("Test true  test ", result)
    }

    @Test
    fun `smartFormat skips empty block`() {
        val pattern = "Test[ \${foo}]"

        val result = Formatter.create(pattern).set("foo", null).smartFormat()

        assertEquals("Test", result)
    }

    @Test
    fun `smartFormat accepts nested blocks`() {
        val pattern = "Test[[ \${foo}] bar \${baz}]"

        val result1 = Formatter.create(pattern).set("foo", null).set("baz", null).smartFormat()
        val result2 = Formatter.create(pattern).set("foo", "foo").set("baz", null).smartFormat()
        val result3 = Formatter.create(pattern).set("foo", null).set("baz", "baz").smartFormat()
        val result4 = Formatter.create(pattern).set("foo", "foo").set("baz", "baz").smartFormat()

        assertEquals("Test", result1)
        assertEquals("Test foo bar ", result2)
        assertEquals("Test bar baz", result3)
        assertEquals("Test foo bar baz", result4)
    }

    @Test
    fun `format fails when using unknown parameter`() {
        val pattern = "Test \${foo}"

        assertThrows(IllegalArgumentException::class.java) {
            Formatter.create(pattern).smartFormat()
        }
    }

    @Test
    fun `format fails for missing curly bracket`() {
        val pattern = "Test \${foo"

        assertThrows(IllegalArgumentException::class.java) {
            Formatter.create(pattern).smartFormat()
        }
    }

    @Test
    fun `format fails for missing square bracket`() {
        val pattern = "Test [\${foo}"

        assertThrows(IllegalArgumentException::class.java) {
            Formatter.create(pattern).smartFormat()
        }
    }

    @Test
    fun `smartFormat fails for additional square bracket`() {
        val pattern = "Test [\${foo}]]"

        assertThrows(IllegalArgumentException::class.java) {
            Formatter.create(pattern).smartFormat()
        }
    }

    @Test
    fun `createJSFormatter works`() {
        val pattern = "foo = '\${foo}' bar = \"\${bar}\""

        val result = Formatter.createJSFormatter(pattern).set("foo", "d'or").set("bar", "\"buzz\"").format()

        assertEquals(
                """
                    foo = 'd\'or' bar = "\"buzz\""
                """.trimIndent(), result
        )
    }
}
