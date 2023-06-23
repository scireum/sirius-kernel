package sirius.kernel.commons;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
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
        assertEquals("Hello", Strings.reduceCharacters("Hello"));
        assertSame("Hello", Strings.reduceCharacters("Hello"));
        assertEquals("Hello", Strings.reduceCharacters("Héllo"));
        assertEquals("AOEO", Strings.reduceCharacters("AÖO"));
        assertEquals("AEAAE", Strings.reduceCharacters("ÄAÄ"));
    }

    @Test
    void cleanup() {
        assertEquals("Hel lo", Strings.cleanup("Hel lo ", EnumSet.of(Cleanup.TRIM)));
        assertEquals("Hel lo ", Strings.cleanup("Hel  lo ", EnumSet.of(Cleanup.REDUCE_WHITESPACES)));
        assertEquals("Hello", Strings.cleanup("Hel  lo", EnumSet.of(Cleanup.REMOVE_WHITESPACES)));
        assertEquals("Hello", Strings.cleanup("Héllo", EnumSet.of(Cleanup.REDUCE_CHARACTERS)));
        assertEquals("hello", Strings.cleanup("Héllo", EnumSet.of(Cleanup.REDUCE_CHARACTERS, Cleanup.LOWERCASE)));
        assertEquals("HELLO", Strings.cleanup("Héllo", EnumSet.of(Cleanup.REDUCE_CHARACTERS, Cleanup.UPPERCASE)));
        assertEquals("Hello", Strings.cleanup("Hel-lo", EnumSet.of(Cleanup.REMOVE_PUNCTUATION)));
        assertEquals("Hello", Strings.cleanup("\10Hello", EnumSet.of(Cleanup.REMOVE_CONTROL_CHARS)));
    }
}
