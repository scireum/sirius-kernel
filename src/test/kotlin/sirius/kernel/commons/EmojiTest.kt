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
class EmojiTest {

    @Test
    fun detectsSingleEmojisCorrectly() {
        assertTrue { Emojis.isEmoji("👩🏾‍🚀") }
        assertTrue { Emojis.isEmoji("🚒") }
        assertTrue { Emojis.isEmoji("🌾") }
        assertTrue { Emojis.isEmoji("😇") }
        assertTrue { Emojis.isEmoji("🫛") }
        assertTrue { Emojis.isEmoji("🐕‍🦺") }
        assertTrue { Emojis.isEmoji("🇫🇷") }
        assertTrue { Emojis.isEmoji("🥨") }
        assertTrue { Emojis.isEmoji("🇿🇼") }
        assertTrue { Emojis.isEmoji("🔣") }
        assertTrue { Emojis.isEmoji("🎎") }
        assertTrue { Emojis.isEmoji("🥺") }

        assertFalse { Emojis.isEmoji("") }
        assertFalse { Emojis.isEmoji("a") }
        assertFalse { Emojis.isEmoji("👍👍") }
    }

    @Test
    fun detectsPresenceOfEmojisCorrectly() {
        assertTrue { Emojis.containsEmoji("Spaceflight! 👩🏾‍🚀") }
        assertTrue { Emojis.containsEmoji("Yes 👍👍") }
        assertTrue { Emojis.containsEmoji("😇") }
        assertTrue { Emojis.containsEmoji("👍👍") }

        assertFalse { Emojis.containsEmoji("") }
        assertFalse { Emojis.containsEmoji("Nope") }
    }

    @Test
    fun detectsMultipleEmojisCorrectly() {
        assertTrue { Emojis.onlyContainsEmojis("👩🏾‍🚀") }
        assertTrue { Emojis.onlyContainsEmojis("👍👍") }
        assertTrue { Emojis.onlyContainsEmojis("👩🏾‍🚀🪐🚀") }

        assertFalse { Emojis.onlyContainsEmojis("") }
        assertFalse { Emojis.onlyContainsEmojis("Nope") }
        assertFalse { Emojis.onlyContainsEmojis("Nope 👎👎") }
        assertFalse { Emojis.onlyContainsEmojis("👎👎 Nope") }
        assertFalse { Emojis.onlyContainsEmojis(" 👎👎") }
        assertFalse { Emojis.onlyContainsEmojis("👎 👎") }
        assertFalse { Emojis.onlyContainsEmojis("👎👎 ") }
    }

    @Test
    fun countsEmojisCorrectly() {
        assertEquals(0, Emojis.countEmojis("Hallo"))
        assertEquals(1, Emojis.countEmojis("Hallo 🙂"))
        assertEquals(1, Emojis.countEmojis("🥳"))
        assertEquals(1, Emojis.countEmojis("🍾 Yay!"))
        assertEquals(1, Emojis.countEmojis("Hallo 🙂 Wie geht's?"))
        assertEquals(2, Emojis.countEmojis("Hallo 🙂👋"))
        assertEquals(2, Emojis.countEmojis("😅🤦‍♂️"))
        assertEquals(3, Emojis.countEmojis("👩🏾‍🚀🪐🚀"))
    }
}
