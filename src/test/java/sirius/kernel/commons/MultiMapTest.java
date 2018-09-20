package sirius.kernel.commons;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class MultiMapTest {
    @Test
    public void test() {
        MultiMap<String, String> mm = MultiMap.create();
        mm.put("A", "A");
        mm.put("A", "B");
        assertArrayEquals(new String[]{"A", "B"}, mm.get("A").toArray(new String[mm.get("A").size()]));
        mm.put("A", "B");
        assertArrayEquals(new String[]{"A", "B", "B"}, mm.get("A").toArray(new String[mm.get("A").size()]));
        mm.remove("A", "B");
        assertArrayEquals(new String[]{"A"}, mm.get("A").toArray(new String[mm.get("A").size()]));
        mm.put("B", "A");
        mm.put("B", "C");
        assertArrayEquals(new String[]{"A", "B"}, mm.keySet().toArray(new String[mm.keySet().size()]));
        assertArrayEquals(new String[]{"A", "A", "C"}, mm.values().toArray(new String[mm.values().size()]));
        mm.getUnderlyingMap().clear();
        assertArrayEquals(new String[0], mm.get("A").toArray(new String[mm.get("A").size()]));
        mm.put("B", "C");
        assertArrayEquals(new String[]{"C"}, mm.get("B").toArray(new String[mm.get("B").size()]));
        mm.clear();
        assertArrayEquals(new String[0], mm.get("B").toArray(new String[mm.get("B").size()]));
    }
}
