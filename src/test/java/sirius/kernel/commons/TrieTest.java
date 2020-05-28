package sirius.kernel.commons;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.*;

public class TrieTest {

    private Trie<Integer> trie;

    @Before
    public void createTrie() {
        trie = Trie.create();
        trie.put("one", 1);
        trie.put("on", 2);
        trie.put("one1", 3);
        trie.put("two", 4);
        trie.put("three", 5);
        trie.put("thrae", 6);
        trie.put("th", 7);
    }

    @Test
    public void isFilled() {
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

    @Test
    public void keySet() {
        assertEquals(7, trie.size());
        assertEquals(new HashSet<>(Arrays.asList("one", "on", "one1", "two", "three", "thrae", "th")), trie.keySet());
        assertEquals(new HashSet<>(Arrays.asList("one", "on", "one1", "two", "three", "thrae", "th")),
                     trie.getAllKeysBeginningWith(""));
        assertEquals(new HashSet<>(Arrays.asList("one", "on", "one1")), trie.getAllKeysBeginningWith("on"));
        assertEquals(Collections.singleton("three"), trie.getAllKeysBeginningWith("three"));
        assertEquals(0, trie.getAllKeysBeginningWith("threee").size());
    }
}
