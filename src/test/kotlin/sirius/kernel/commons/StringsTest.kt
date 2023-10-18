/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons

import org.junit.jupiter.api.Test
import java.util.*
import java.util.function.UnaryOperator
import java.util.regex.Pattern
import kotlin.test.*

/**
 * Tests the [Strings] class.
 */
class StringsTest {

    @Test
    fun isFilled() {
        assertTrue { Strings.isFilled("Test") }
        assertTrue { Strings.isFilled(" ") }
        assertFalse { Strings.isFilled(null) }
        assertFalse { Strings.isFilled("") }
    }

    @Test
    fun isEmpty() {
        assertFalse { Strings.isEmpty("Test") }
        assertFalse { Strings.isEmpty(" ") }
        assertTrue { Strings.isEmpty(null) }
        assertTrue { Strings.isEmpty("") }
    }

    @Test
    fun equalIgnoreCase() {
        assertTrue { Strings.equalIgnoreCase("A", "a") }
        assertFalse { Strings.equalIgnoreCase("A", "b") }
        assertTrue { Strings.equalIgnoreCase("", null) }
        assertFalse { Strings.equalIgnoreCase(" ", null) }
        assertTrue { Strings.equalIgnoreCase(null, null) }
    }

    @Test
    fun areEqual() {
        assertTrue { Strings.areEqual("A", "A") }
        assertFalse { Strings.areEqual("a", "A") }
        assertFalse { Strings.areEqual("a", "A") }
        assertTrue {
            Strings.areEqual(
                    "a", "A"
            ) { x: Any -> x.toString().lowercase(Locale.getDefault()) }
        }
        assertTrue { Strings.areEqual("", null) }
        assertFalse { Strings.areEqual(" ", null) }
        assertTrue { Strings.areEqual(null, null) }
    }

    @Test
    fun areTrimmedEqual() {
        assertTrue { Strings.areTrimmedEqual("A ", "A") }
        assertFalse { Strings.areTrimmedEqual("a", "A  ") }
        assertTrue { Strings.areTrimmedEqual("", null) }
        assertTrue { Strings.areTrimmedEqual(" ", null) }
        assertTrue { Strings.areTrimmedEqual(null, null) }
    }

    @Test
    fun toStringMethod() {
        assertEquals("A", Strings.toString("A"))
        assertEquals("", Strings.toString(""))
        assertNull(Strings.toString(null))
    }

    @Test
    fun apply() {
        assertEquals("B A", Strings.apply("%s A", "B"))
        assertEquals("A null", Strings.apply("A %s", null as String?))
    }

    @Test
    fun firstFilled() {
        assertEquals("A", Strings.firstFilled("A"))
        assertEquals("A", Strings.firstFilled("A", "B"))
        assertEquals("A", Strings.firstFilled(null, "A"))
        assertEquals("A", Strings.firstFilled("", "A"))
        assertEquals("A", Strings.firstFilled(null, null, "A"))
        assertNull(Strings.firstFilled())
        assertNull(Strings.firstFilled(null as String?))
        assertNull(Strings.firstFilled(""))
    }

    @Test
    fun urlEncode() {
        assertEquals("A%3FTEST%26B%C3%84%C3%96%C3%9C", Strings.urlEncode("A?TEST&BÄÖÜ"))
    }

    @Test
    fun urlDecode() {
        assertEquals("A?TEST&BÄÖÜ", Strings.urlDecode("A%3FTEST%26B%C3%84%C3%96%C3%9C"))
    }

    @Test
    fun split() {
        assertEquals(Tuple.create("A", "B"), Strings.split("A|B", "|"))
        assertEquals(Tuple.create("A", "&B"), Strings.split("A&&B", "&"))
        assertEquals(Tuple.create("A", "B"), Strings.split("A&&B", "&&"))
        assertEquals(Tuple.create("A", ""), Strings.split("A|", "|"))
        assertEquals(Tuple.create("", "B"), Strings.split("|B", "|"))
        assertEquals(Tuple.create<String, String?>("A&B", null), Strings.split("A&B", "|"))
    }

    @Test
    fun splitSmart() {
        assertEquals(listOf("a"), Strings.splitSmart("a", 2))
        assertEquals(listOf<Any>(), Strings.splitSmart(null, 0))
        assertEquals(listOf<Any>(), Strings.splitSmart(null, 2))
        assertEquals(listOf<Any>(), Strings.splitSmart("", 0))
        assertEquals(listOf<Any>(), Strings.splitSmart("", 2))
        assertEquals(listOf("das ist", "ein", "Test"), Strings.splitSmart("das ist ein Test", 7))
        assertEquals(
                listOf("lange-w", "örter-w", "erden-a", "uch-get", "rennt"),
                Strings.splitSmart("lange-wörter-werden-auch-getrennt", 7)
        )
        assertEquals(
                listOf("Ein langer Text kann in eine Zeile"),
                Strings.splitSmart("Ein langer Text kann in eine Zeile", 40)
        )
    }

    @Test
    fun join() {
        assertEquals("A,B,C", Strings.join(",", "A", "B", "C"))
        assertEquals("A,C", Strings.join(",", "A", null, "", "C"))
        assertEquals("A", Strings.join(",", "A"))
        assertEquals("", Strings.join(","))
        assertEquals("ABC", Strings.join("", "A", "B", "C"))
    }

    @Test
    fun replaceAll() {
        assertEquals("A&lt;B&amp;C&amp;&amp;D&amp;;&amp;E", Strings.replaceAll(
                Pattern.compile("&([a-zA-Z0-9]{0,6};?)"), "A&lt;B&C&&D&;&E"
        ) { s: String -> (if (s.endsWith(";") && !s.startsWith(";")) "&" else "&amp;") + s })
    }

    @Test
    fun leftPad() {
        assertEquals("   A", Strings.leftPad("A", " ", 4))
        assertEquals("    A", Strings.leftPad("A", "  ", 5))
        assertEquals("    A", Strings.leftPad("A", "  ", 4))
        assertEquals("AAA", Strings.leftPad("AAA", " ", 2))
    }

    @Test
    fun rightPad() {
        assertEquals("A   ", Strings.rightPad("A", " ", 4))
        assertEquals("A    ", Strings.rightPad("A", "  ", 5))
        assertEquals("A    ", Strings.rightPad("A", "  ", 4))
        assertEquals("AAA", Strings.rightPad("AAA", " ", 2))
    }

    @Test
    fun reduceCharacters() {
        assertEquals("Hello", StringCleanup.reduceCharacters("Hello"))
        assertSame("Hello", StringCleanup.reduceCharacters("Hello"))
        assertEquals("Hello", StringCleanup.reduceCharacters("Héllo"))
        assertEquals("AOEO", StringCleanup.reduceCharacters("AÖO"))
        assertEquals("AEAAE", StringCleanup.reduceCharacters("ÄAÄ"))
    }

    @Test
    fun cleanup() {
        assertEquals(
                "Hel lo", Strings.cleanup("Hel lo ", UnaryOperator { input: String? -> StringCleanup.trim(input!!) })
        )
        assertEquals(
                "Hel lo ",
                Strings.cleanup(
                        "Hel \t \t \r\n lo ",
                        UnaryOperator { input: String? -> StringCleanup.reduceWhitespace(input!!) })
        )
        assertEquals(
                "Hello",
                Strings.cleanup(
                        "Hel \t \t \n lo ",
                        UnaryOperator { input: String? -> StringCleanup.removeWhitespace(input!!) })
        )
        assertEquals(
                "Hello",
                Strings.cleanup("Héllo", UnaryOperator { term: String? -> StringCleanup.reduceCharacters(term) })
        )
        assertEquals(
                "hello", Strings.cleanup("Héllo",
                UnaryOperator { term: String? -> StringCleanup.reduceCharacters(term) },
                UnaryOperator { input: String? -> StringCleanup.lowercase(input!!) })
        )
        assertEquals(
                "HELLO", Strings.cleanup("Héllo",
                UnaryOperator { term: String? -> StringCleanup.reduceCharacters(term) },
                UnaryOperator { input: String? -> StringCleanup.uppercase(input!!) })
        )
        assertEquals(
                "Hello",
                Strings.cleanup("Hel-lo", UnaryOperator { input: String? -> StringCleanup.removePunctuation(input!!) })
        )
        assertEquals(
                "Hello",
                Strings.cleanup(
                        "\u0008Hello",
                        UnaryOperator { input: String? -> StringCleanup.removeControlCharacters(input!!) })
        )
        assertEquals(
                "Test", Strings.cleanup("<b>Test</b>",
                UnaryOperator { input: String? -> StringCleanup.replaceXml(input) },
                UnaryOperator { input: String? -> StringCleanup.trim(input!!) })
        )
        assertEquals(
                "Test", Strings.cleanup("<b>Test<br><img /></b>",
                UnaryOperator { input: String? -> StringCleanup.replaceXml(input) },
                UnaryOperator { input: String? -> StringCleanup.trim(input!!) })
        )
        assertEquals(
                "Test Blubb", Strings.cleanup("<b>Test<br><img />Blubb</b>",
                UnaryOperator { input: String? -> StringCleanup.replaceXml(input) },
                UnaryOperator { input: String? -> StringCleanup.trim(input!!) })
        )
        assertEquals(
                "foo having < 3 m, with >= 3 m", Strings.cleanup("foo having < 3 m, with >= 3 m",
                UnaryOperator { input: String? -> StringCleanup.replaceXml(input) },
                UnaryOperator { input: String? -> StringCleanup.trim(input!!) })
        )
        assertEquals(
                "&lt;b&gt;Foo &lt;br /&gt; Bar&lt;/b&gt;",
                Strings.cleanup("<b>Foo <br /> Bar</b>", UnaryOperator { input: String? ->
                    StringCleanup.escapeXml(
                            input
                    )
                })
        )
        assertEquals(
                "Hello <br> World",
                Strings.cleanup("Hello\nWorld", UnaryOperator { input: String? -> StringCleanup.nlToBr(input) })
        )
        assertEquals(
                "Testalert('Hello World!')", Strings.cleanup("Test<script>alert('Hello World!')</script>",
                UnaryOperator { input: String? -> StringCleanup.removeXml(input!!) })
        )
        assertEquals(
                " äöüÄÖÜß<>\"'&* * * * * * ",
                Strings.cleanup("&nbsp;&auml;&ouml;&uuml;&Auml;&Ouml;&Uuml;&szlig;&lt;&gt;&quot;&apos;&amp;&#8226;&#8226;&#8227;&#8227;&#8259;&#8259;",
                        UnaryOperator { input: String? -> StringCleanup.decodeHtmlEntities(input!!) })
        )
    }

    @Test
    fun probablyContainsXml() {
        assertTrue(Strings.probablyContainsXml("<b>Test</b>"))
        assertTrue(Strings.probablyContainsXml("<br>"))
        assertTrue(Strings.probablyContainsXml("<br />"))
        assertTrue(Strings.probablyContainsXml("<br test=\"foo\">"))
        assertTrue(Strings.probablyContainsXml("</div>"))
        assertTrue(Strings.probablyContainsXml("<namespace:element>"))
        assertTrue(Strings.probablyContainsXml("test&amp;test"))
        assertTrue(Strings.probablyContainsXml("test&frac12;test"))
        assertTrue(Strings.probablyContainsXml("test&#x00F7;test"))
        assertFalse(Strings.probablyContainsXml("foo having < 3 m, with >= 3 m"))
        assertFalse(Strings.probablyContainsXml("foo having <3 m, with > 3 m"))
        assertFalse(Strings.probablyContainsXml("test & amp ; test"))
    }

    @Test
    fun containsAllowedHtml() {
        assertTrue(Strings.containsAllowedHtml("<b>Test</b>"))
        assertTrue(Strings.containsAllowedHtml("<br>"))
        assertTrue(Strings.containsAllowedHtml("<br />"))
        assertTrue(Strings.containsAllowedHtml("<br test=\"foo\">"))
        assertTrue(Strings.containsAllowedHtml("</div>"))
        assertTrue(Strings.containsAllowedHtml("<div><script>Test</script></div>"))
        assertFalse(Strings.containsAllowedHtml("<namespace:element>"))
        assertFalse(Strings.containsAllowedHtml("Test <script>alert('Hello World!')</script>"))
        assertFalse(Strings.containsAllowedHtml("foo having < 3 m, with >= 3 m"))
        assertFalse(Strings.containsAllowedHtml("foo having <3 m, with > 3 m"))
    }

    @Test
    fun limit() {
        assertEquals("", Strings.limit(null, 10, false))
        assertEquals("", Strings.limit(null, 10, true))
        assertEquals("", Strings.limit("", 10, false))
        assertEquals("", Strings.limit("", 10, true))
        assertEquals("ABCDE", Strings.limit("ABCDE", 10, false))
        assertEquals("ABCDE", Strings.limit("ABCDE", 10, true))
        assertEquals("ABCDEFGHIJ", Strings.limit("ABCDEFGHIJKLMNOP", 10, false))
        assertEquals("ABCDEFGHI…", Strings.limit("ABCDEFGHIJKLMNOP", 10, true))
    }

}
