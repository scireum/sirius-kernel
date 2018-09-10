package sirius.kernel.commons;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class PriorityCollectorTest {
    @Test
    public void test() {
        PriorityCollector<String> c = PriorityCollector.create();
        c.addDefault("B");
        c.add(50, "A");
        c.add(101, "C");
        assertEquals(Arrays.asList("A", "B", "C"), c.getData());
        c.getData().clear();
        assertEquals(3, c.size());
    }
}
