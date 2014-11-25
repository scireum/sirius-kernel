package sirius.kernel.commons;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class ValueTest {
    @Test
    public void isFilled() {
        assertTrue(Value.of(1).isFilled());
        assertTrue(Value.of(" ").isFilled());
        assertFalse(Value.of("").isFilled());
        assertFalse(Value.of(null).isFilled());
    }

    @Test
    public void strings() {
        assertEquals("pdf", Value.of("test.x.pdf").afterLast("."));
        assertEquals("test.x", Value.of("test.x.pdf").beforeLast("."));
        assertEquals("x.pdf", Value.of("test.x.pdf").afterFirst("."));
        assertEquals("test", Value.of("test.x.pdf").beforeFirst("."));

        assertEquals("testA", Value.of("testA.testB").left(5));
        assertEquals(".testB", Value.of("testA.testB").left(-5));
        assertEquals("testB", Value.of("testA.testB").right(5));
        assertEquals("testA.", Value.of("testA.testB").right(-5));
        assertEquals("", Value.of(null).right(-5));
    }
}
