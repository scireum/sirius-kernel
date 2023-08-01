/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests the [Trie] class.
 */
class TrieTest {
    @Test
    fun isFilled() {
        val check = "I'd like to have three beer please"
        val iter = trie.iterator()
        var found = 0
        for (index in check.indices) {
            if (!iter.doContinue(check[index].code)) {
                if (iter.isCompleted()) {
                    found = iter.getValue()
                }
                iter.resetWith(check[index].code)
            }
        }
        assertEquals(5, found)
        assertEquals(2, trie["on"] as Int)
        assertNull(trie["onx"])
        assertTrue { trie.containsKey("thrae") }
        assertFalse { trie.containsKey("thre") }
    }

    @Test
    fun keySet() {
        assertEquals(7, trie.size())
        assertEquals(setOf("one", "on", "one1", "two", "three", "thrae", "th"), trie.keySet())
        assertEquals(setOf("one", "on", "one1", "two", "three", "thrae", "th"), trie.getAllKeysBeginningWith(""))
        assertEquals(setOf("one", "on", "one1"), trie.getAllKeysBeginningWith("on"))
        assertEquals(setOf("three"), trie.getAllKeysBeginningWith("three"))
        assertEquals(0, trie.getAllKeysBeginningWith("threee").size)
    }

    companion object {
        private lateinit var trie: Trie<Int>

        @JvmStatic
        @BeforeAll
        fun createTrie() {
            trie = Trie.create()
            trie.put("one", 1)
            trie.put("on", 2)
            trie.put("one1", 3)
            trie.put("two", 4)
            trie.put("three", 5)
            trie.put("thrae", 6)
            trie.put("th", 7)
        }
    }
}
