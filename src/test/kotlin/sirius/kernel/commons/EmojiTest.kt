/*
 * Made with all the love in the world
 * by scireum in Stuttgart, Germany
 *
 * Copyright by scireum GmbH
 * https://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the [Emojis] class.
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
        assertTrue { Emojis.isEmoji("🫆") }

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
    fun detectsMultipleEmojisWithWhitespaceCorrectly() {
        assertTrue { Emojis.onlyContainsEmojisWithWhitespace("👩🏾‍🚀") }
        assertTrue { Emojis.onlyContainsEmojisWithWhitespace("👍👍") }
        assertTrue { Emojis.onlyContainsEmojisWithWhitespace("👩🏾‍🚀🪐🚀") }
        assertTrue { Emojis.onlyContainsEmojisWithWhitespace(" 👎👎") }
        assertTrue { Emojis.onlyContainsEmojisWithWhitespace("👎 👎") }
        assertTrue { Emojis.onlyContainsEmojisWithWhitespace("👎👎 ") }

        assertFalse { Emojis.onlyContainsEmojisWithWhitespace("") }
        assertFalse { Emojis.onlyContainsEmojisWithWhitespace(" ") }
        assertFalse { Emojis.onlyContainsEmojisWithWhitespace("Nope") }
        assertFalse { Emojis.onlyContainsEmojisWithWhitespace("Nope 👎👎") }
        assertFalse { Emojis.onlyContainsEmojisWithWhitespace("👎👎 Nope") }
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

    @Test
    fun removesEmojisCorrectly() {
        assertEquals("", Emojis.removeEmojis("🥳"))
        assertEquals("", Emojis.removeEmojis("😅🤦‍♂️"))
        assertEquals("", Emojis.removeEmojis("👩🏾‍🚀🪐🚀"))
        assertEquals("Hallo", Emojis.removeEmojis("Hallo"))
        assertEquals("Hallo ", Emojis.removeEmojis("Hallo 🙂"))
        assertEquals("Hallo ", Emojis.removeEmojis("Hallo 🙂👋"))
        assertEquals(" Yay!", Emojis.removeEmojis("🍾 Yay!"))
        assertEquals(" Yay!", Emojis.removeEmojis("🍾🥂 Yay!"))
        assertEquals("Hallo  Wie geht's?", Emojis.removeEmojis("Hallo 🙂 Wie geht's?"))
        assertEquals("Hallo  Wie geht's?", Emojis.removeEmojis("Hallo 🙂👋 Wie geht's?"))
    }
}
