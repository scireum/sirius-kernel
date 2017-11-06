package sirius.kernel.commons;

import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class StringsTest {

    @Test
    public void isFilled() {
        assertTrue(Strings.isFilled("Test"));
        assertTrue(Strings.isFilled(" "));
        assertFalse(Strings.isFilled(null));
        assertFalse(Strings.isFilled(""));
    }

    @Test
    public void isEmpty() {
        assertFalse(Strings.isEmpty("Test"));
        assertFalse(Strings.isEmpty(" "));
        assertTrue(Strings.isEmpty(null));
        assertTrue(Strings.isEmpty(""));
    }

    @Test
    public void equalIgnoreCase() {
        assertTrue(Strings.equalIgnoreCase("A", "a"));
        assertFalse(Strings.equalIgnoreCase("A", "b"));
        assertTrue(Strings.equalIgnoreCase("", null));
        assertFalse(Strings.equalIgnoreCase(" ", null));
        assertTrue(Strings.equalIgnoreCase(null, null));
    }

    @Test
    public void areEqual() {
        assertTrue(Strings.areEqual("A", "A"));
        assertFalse(Strings.areEqual("a", "A"));
        assertTrue(Strings.areEqual("", null));
        assertFalse(Strings.areEqual(" ", null));
        assertTrue(Strings.areEqual(null, null));
    }

    @Test
    public void toStringMethod() {
        assertEquals("A", Strings.toString("A"));
        assertEquals("", Strings.toString(""));
        assertNull(Strings.toString(null));
    }

    @Test
    public void apply() {
        assertEquals("B A", Strings.apply("%s A", "B"));
        assertEquals("A null", Strings.apply("A %s", (String) null));
    }

    @Test
    public void firstFilled() {
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
    public void urlEncode() {
        assertEquals("A%3FTEST%26B%C3%84%C3%96%C3%9C", Strings.urlEncode("A?TEST&BÄÖÜ"));
    }

    @Test
    public void split() {
        assertEquals(Tuple.create("A", "B"), Strings.split("A|B", "|"));
        assertEquals(Tuple.create("A", "&B"), Strings.split("A&&B", "&"));
        assertEquals(Tuple.create("A", "B"), Strings.split("A&&B", "&&"));
        assertEquals(Tuple.create("A", ""), Strings.split("A|", "|"));
        assertEquals(Tuple.create("", "B"), Strings.split("|B", "|"));
        assertEquals(Tuple.create("A&B", null), Strings.split("A&B", "|"));
    }

    @Test
    public void join() {
        assertEquals("A,B,C", Strings.join(",", "A", "B", "C"));
        assertEquals("A,C", Strings.join(",", "A", null, "", "C"));
        assertEquals("A", Strings.join(",", "A"));
        assertEquals("", Strings.join(","));
        assertEquals("ABC", Strings.join("", "A", "B", "C"));
    }

    @Test
    public void replaceAll() {
        assertEquals("A&lt;B&amp;C&amp;&amp;D&amp;;&amp;E",
                     Strings.replaceAll(Pattern.compile("&([a-zA-Z0-9]{0,6};?)"),
                                        "A&lt;B&C&&D&;&E",
                                        s -> (s.endsWith(";") && !s.startsWith(";") ? "&" : "&amp;") + s));
    }

    @Test
    public void leftPad() {
        assertEquals("   A", Strings.leftPad("A", " ", 4));
        assertEquals("    A", Strings.leftPad("A", "  ", 5));
        assertEquals("    A", Strings.leftPad("A", "  ", 4));
        assertEquals("AAA", Strings.leftPad("AAA", " ", 2));
    }

    @Test
    public void rightPad() {
        assertEquals("A   ", Strings.rightPad("A", " ", 4));
        assertEquals("A    ", Strings.rightPad("A", "  ", 5));
        assertEquals("A    ", Strings.rightPad("A", "  ", 4));
        assertEquals("AAA", Strings.rightPad("AAA", " ", 2));
    }

    @Test
    public void trim() {
        assertNull(Strings.trim(null));
        assertNull(Strings.trim(""));
        assertEquals("A", Strings.trim("  A  "));
        assertEquals("A", Strings.trim("\u0000\t\n\rA\u0000\t\n\r"));
        assertEquals("Blubb   : 3", Strings.trim(Tuple.create("   Blubb   ", 3)));

        assertNull(Strings.trim(null));
        assertNull(Strings.trim(""));
        assertEquals("A", Strings.trim("  A  ", " "));
        assertEquals("D", Strings.trim("  AD  ", " ABC"));
        assertEquals("\n\rA\u0000\t\n", Strings.trim("\u0000\t\n\rA\u0000\t\n\r", "\t\r\u0000"));
    }

    @Test
    public void ltrim() {
        assertNull(Strings.ltrim(null, " "));
        assertNull(Strings.ltrim("", " "));
        assertEquals("A  ", Strings.ltrim("  A  ", " "));
        assertEquals("", Strings.ltrim("  A  ", " ABC"));
        assertEquals("D  ", Strings.ltrim("  AD  ", " ABC"));
        assertEquals("A\u0000\t\n\r", Strings.ltrim("\u0000\t\n\rA\u0000\t\n\r", "\r\t\n\u0000\t\n\r"));
        assertEquals("Blubb   : 3", Strings.ltrim(Tuple.create("   Blubb   ", 3), " "));
        assertEquals("\n\rA\u0000\t\n\r", Strings.ltrim("\u0000\t\n\rA\u0000\t\n\r", "\t\r\u0000"));
    }

    @Test
    public void rtrim() {
        assertNull(Strings.rtrim(null, " "));
        assertNull(Strings.rtrim("", " "));
        assertEquals("  A", Strings.rtrim("  A  ", " "));
        assertEquals("", Strings.rtrim("  A  ", " ABC"));
        assertEquals("  D", Strings.rtrim("  DA  ", " ABC"));
        assertEquals("\u0000\t\n\rA", Strings.rtrim("\u0000\t\n\rA\u0000\t\n\r", "\r\t\n\u0000\t\n\r"));
        assertEquals("3:    Blubb", Strings.rtrim(Tuple.create(3, "   Blubb   "), " "));
        assertEquals("\u0000\t\n\rA\u0000\t", Strings.rtrim("\u0000\t\n\rA\u0000\t\n\r", "\n\r\u0000"));
    }
}
