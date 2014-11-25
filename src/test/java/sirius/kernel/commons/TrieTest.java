package sirius.kernel.commons;

import org.junit.Test;

import static org.junit.Assert.*;

public class TrieTest {
    @Test
    public void isFilled() {
        Trie<Integer> trie = Trie.create();

        trie.put("one", 1);
        trie.put("on", 2);
        trie.put("one1", 3);
        trie.put("two", 4);
        trie.put("three", 5);
        trie.put("thrae", 6);
        trie.put("th", 7);

        String check = "I'd like to have three beer please";
        Trie.ContainmentIterator<Integer> iter = trie.iterator();

        int found = 0;

        for (int i = 0; i < check.length(); i++) {
            if (!iter.doContinue(check.charAt(i))) {
                if (iter.isCompleted()) {
                    found = iter.getValue();
                }
                iter.resetWith(check.charAt(i));
            }
        }

        assertEquals(5, found);

        assertEquals(2, (int) trie.get("on"));
        assertEquals(null, trie.get("onx"));
        assertTrue(trie.containsKey("thrae"));
        assertFalse(trie.containsKey("thre"));
    }
}
