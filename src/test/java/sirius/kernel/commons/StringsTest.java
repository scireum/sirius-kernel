package sirius.kernel.commons;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class StringsTest {

    @Test
    void isFilled() {
        assertTrue(Strings.isFilled("Test"));
        assertTrue(Strings.isFilled(" "));
        assertFalse(Strings.isFilled(null));
        assertFalse(Strings.isFilled(""));
    }

    @Test
    void isEmpty() {
        assertFalse(Strings.isEmpty("Test"));
        assertFalse(Strings.isEmpty(" "));
        assertTrue(Strings.isEmpty(null));
        assertTrue(Strings.isEmpty(""));
    }

    @Test
    void equalIgnoreCase() {
        assertTrue(Strings.equalIgnoreCase("A", "a"));
        assertFalse(Strings.equalIgnoreCase("A", "b"));
        assertTrue(Strings.equalIgnoreCase("", null));
        assertFalse(Strings.equalIgnoreCase(" ", null));
        assertTrue(Strings.equalIgnoreCase(null, null));
    }

    @Test
    void areEqual() {
        assertTrue(Strings.areEqual("A", "A"));
        assertFalse(Strings.areEqual("a", "A"));
        assertFalse(Strings.areEqual("a", "A"));
        assertTrue(Strings.areEqual("a", "A", x -> x.toString().toLowerCase()));
        assertTrue(Strings.areEqual("", null));
        assertFalse(Strings.areEqual(" ", null));
        assertTrue(Strings.areEqual(null, null));
    }

    @Test
    void areTrimmedEqual() {
        assertTrue(Strings.areTrimmedEqual("A ", "A"));
        assertFalse(Strings.areTrimmedEqual("a", "A  "));
        assertTrue(Strings.areTrimmedEqual("", null));
        assertTrue(Strings.areTrimmedEqual(" ", null));
        assertTrue(Strings.areTrimmedEqual(null, null));
    }

    @Test
    void toStringMethod() {
        assertEquals("A", Strings.toString("A"));
        assertEquals("", Strings.toString(""));
        assertNull(Strings.toString(null));
    }

    @Test
    void apply() {
        assertEquals("B A", Strings.apply("%s A", "B"));
        assertEquals("A null", Strings.apply("A %s", (String) null));
    }

    @Test
    void firstFilled() {
        assertEquals("A", Strings.firstFilled("A"));
        assertEquals("A", Strings.firstFilled("A", "B"));
        assertEquals("A", Strings.firstFilled(null, "A"));
        assertEquals("A", Strings.firstFilled("", "A"));
        assertEquals("A", Strings.firstFilled(null, null, "A"));
        assertNull(Strings.firstFilled());
        assertNull(Strings.firstFilled((String) null));
        assertNull(Strings.firstFilled(""));
    }

    @Test
    void urlEncode() {
        assertEquals("A%3FTEST%26B%C3%84%C3%96%C3%9C", Strings.urlEncode("A?TEST&BÄÖÜ"));
    }

    @Test
    void urlDecode() {
        assertEquals("A?TEST&BÄÖÜ", Strings.urlDecode("A%3FTEST%26B%C3%84%C3%96%C3%9C"));
    }

    @Test
    void split() {
        assertEquals(Tuple.create("A", "B"), Strings.split("A|B", "|"));
        assertEquals(Tuple.create("A", "&B"), Strings.split("A&&B", "&"));
        assertEquals(Tuple.create("A", "B"), Strings.split("A&&B", "&&"));
        assertEquals(Tuple.create("A", ""), Strings.split("A|", "|"));
        assertEquals(Tuple.create("", "B"), Strings.split("|B", "|"));
        assertEquals(Tuple.create("A&B", null), Strings.split("A&B", "|"));
    }

    @Test
    void splitSmart() {
        assertEquals(List.of("a"), Strings.splitSmart("a", 2));
        assertEquals(List.of(), Strings.splitSmart(null, 0));
        assertEquals(List.of(), Strings.splitSmart(null, 2));
        assertEquals(List.of(), Strings.splitSmart("", 0));
        assertEquals(List.of(), Strings.splitSmart("", 2));
        assertEquals(List.of("das ist", "ein", "Test"), Strings.splitSmart("das ist ein Test", 7));
        assertEquals(List.of("lange-w", "örter-w", "erden-a", "uch-get", "rennt"),
                     Strings.splitSmart("lange-wörter-werden-auch-getrennt", 7));
        assertEquals(List.of("Ein langer Text kann in eine Zeile"),
                     Strings.splitSmart("Ein langer Text kann in eine Zeile", 40));
    }

    @Test
    void join() {
        assertEquals("A,B,C", Strings.join(",", "A", "B", "C"));
        assertEquals("A,C", Strings.join(",", "A", null, "", "C"));
        assertEquals("A", Strings.join(",", "A"));
        assertEquals("", Strings.join(","));
        assertEquals("ABC", Strings.join("", "A", "B", "C"));
    }

    @Test
    void replaceAll() {
        assertEquals("A&lt;B&amp;C&amp;&amp;D&amp;;&amp;E",
                     Strings.replaceAll(Pattern.compile("&([a-zA-Z0-9]{0,6};?)"),
                                        "A&lt;B&C&&D&;&E",
                                        s -> (s.endsWith(";") && !s.startsWith(";") ? "&" : "&amp;") + s));
    }

    @Test
    void leftPad() {
        assertEquals("   A", Strings.leftPad("A", " ", 4));
        assertEquals("    A", Strings.leftPad("A", "  ", 5));
        assertEquals("    A", Strings.leftPad("A", "  ", 4));
        assertEquals("AAA", Strings.leftPad("AAA", " ", 2));
    }

    @Test
    void rightPad() {
        assertEquals("A   ", Strings.rightPad("A", " ", 4));
        assertEquals("A    ", Strings.rightPad("A", "  ", 5));
        assertEquals("A    ", Strings.rightPad("A", "  ", 4));
        assertEquals("AAA", Strings.rightPad("AAA", " ", 2));
    }

    @Test
    void reduceCharacters() {
        assertEquals("Hello", StringCleanup.reduceCharacters("Hello"));
        assertSame("Hello", StringCleanup.reduceCharacters("Hello"));
        assertEquals("Hello", StringCleanup.reduceCharacters("Héllo"));
        assertEquals("AOEO", StringCleanup.reduceCharacters("AÖO"));
        assertEquals("AEAAE", StringCleanup.reduceCharacters("ÄAÄ"));
    }

    @Test
    void cleanup() {
        assertEquals("Hel lo", Strings.cleanup("Hel lo ", StringCleanup::trim));
        assertEquals("Hel lo ", Strings.cleanup("Hel  lo ", StringCleanup::reduceWhitespace));
        assertEquals("Hello", Strings.cleanup("Hel  lo", StringCleanup::removeWhitespace));
        assertEquals("Hello", Strings.cleanup("Héllo", StringCleanup::reduceCharacters));
        assertEquals("hello", Strings.cleanup("Héllo", StringCleanup::reduceCharacters, StringCleanup::lowercase));
        assertEquals("HELLO", Strings.cleanup("Héllo", StringCleanup::reduceCharacters, StringCleanup::uppercase));
        assertEquals("Hello", Strings.cleanup("Hel-lo", StringCleanup::removePunctuation));
        assertEquals("Hello", Strings.cleanup("\10Hello", StringCleanup::removeControlCharacters));
        assertEquals("Test", Strings.cleanup("<b>Test</b>", StringCleanup::replaceXml, StringCleanup::trim));
        assertEquals("Test", Strings.cleanup("<b>Test<br><img /></b>", StringCleanup::replaceXml, StringCleanup::trim));
        assertEquals("Test Blubb",
                     Strings.cleanup("<b>Test<br><img />Blubb</b>", StringCleanup::replaceXml, StringCleanup::trim));
        assertEquals("foo having < 3 m, with >= 3 m",
                     Strings.cleanup("foo having < 3 m, with >= 3 m", StringCleanup::replaceXml, StringCleanup::trim));
        assertEquals("&lt;b&gt;Foo &lt;br /&gt; Bar&lt;/b&gt;",
                     Strings.cleanup("<b>Foo <br /> Bar</b>", StringCleanup::escapeXml));
        assertEquals("Hello <br> World", Strings.cleanup("Hello\nWorld", StringCleanup::nlToBr));
        assertEquals("Hello World", Strings.cleanup("Hello  World", StringCleanup::reduceNbspCharacters));
        assertEquals("Hello\tWorld", Strings.cleanup("Hello\r\nWorld", StringCleanup::replaceLinebreaksWithTabs));
        assertEquals("Testalert('Hello World!')",
                     Strings.cleanup("Test<script>alert('Hello World!')</script>", StringCleanup::removeHtmlTags));
        assertEquals(" äöüÄÖÜß<>\"'&* * * * * * ",
                     Strings.cleanup(
                             "&nbsp;&auml;&ouml;&uuml;&Auml;&Ouml;&Uuml;&szlig;&lt;&gt;&quot;&apos;&amp;&#8226;&#8226;&#8227;&#8227;&#8259;&#8259;",
                             StringCleanup::decodeHtmlEntities));
    }

    @Test
    void probablyContainsXml() {
        assertTrue(Strings.probablyContainsXml("<b>Test</b>"));
        assertTrue(Strings.probablyContainsXml("<br>"));
        assertTrue(Strings.probablyContainsXml("<br />"));
        assertTrue(Strings.probablyContainsXml("<br test=\"foo\">"));
        assertTrue(Strings.probablyContainsXml("<namespace:element>"));
        assertFalse(Strings.probablyContainsXml("foo having < 3 m, with >= 3 m"));
        assertFalse(Strings.probablyContainsXml("foo length<19. with width > 80"));
    }

    @Test
    void probablyContainsHtml() {
        assertTrue(Strings.probablyContainsHtml("<b>Test</b>"));
        assertTrue(Strings.probablyContainsHtml("<br>"));
        assertTrue(Strings.probablyContainsHtml("<br />"));
        assertTrue(Strings.probablyContainsHtml("<br test=\"foo\">"));
        assertTrue(Strings.probablyContainsHtml("<br test=\"foo\">"));
        assertTrue(Strings.probablyContainsHtml("<namespace:tag>"));
        assertFalse(Strings.probablyContainsHtml("foo having < 3 m, with >= 3 m"));
        assertFalse(Strings.probablyContainsHtml("foo length<19. with width > 80"));
    }

    @Test
    void limit() {
        assertEquals("", Strings.limit(null, 10, false));
        assertEquals("", Strings.limit(null, 10, true));
        assertEquals("", Strings.limit("", 10, false));
        assertEquals("", Strings.limit("", 10, true));
        assertEquals("ABCDE", Strings.limit("ABCDE", 10, false));
        assertEquals("ABCDE", Strings.limit("ABCDE", 10, true));
        assertEquals("ABCDEFGHIJ", Strings.limit("ABCDEFGHIJKLMNOP", 10, false));
        assertEquals("ABCDEFGHI…", Strings.limit("ABCDEFGHIJKLMNOP", 10, true));
    }
}
