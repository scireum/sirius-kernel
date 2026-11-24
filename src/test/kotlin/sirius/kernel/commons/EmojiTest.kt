/*
 * Made with all the love in the world
 * by scireum in Stuttgart, Germany
 *
 * Copyright by scireum GmbH
 * https://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons

import org.junit.jupiter.api.Assumptions
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

    @Test
    fun detectsKeycapsFlagsAndTagSequences() {
        assertTrue { Emojis.isEmoji("1️⃣") }
        assertTrue { Emojis.isEmoji("#️⃣") }
        assertTrue { Emojis.isEmoji("*️⃣") }
        assertTrue { Emojis.isEmoji("🇩🇪") }
        assertTrue { Emojis.isEmoji("🏳️‍🌈") }
        assertTrue { Emojis.isEmoji("🏴󠁧󠁢󠁳󠁣󠁴󠁿") }
        assertFalse { Emojis.isEmoji("🇩🇪🇫🇷") }
        assertEquals(2, Emojis.countEmojis("🇩🇪🇫🇷"))
        assertEquals(3, Emojis.countEmojis("1️⃣2️⃣3️⃣"))
    }

    @Test
    fun treatsTextStyleSymbolsAsEmojis() {
        assertTrue { Emojis.isEmoji("©") }
        assertTrue { Emojis.isEmoji("™") }
        assertTrue { Emojis.isEmoji("❤") }
        assertTrue { Emojis.isEmoji("❤️") }
        assertEquals(1, Emojis.countEmojis("Copyright © scireum"))
    }

    @Test
    fun doesNotTreatPlainDigitsAndSymbolsAsEmojis() {
        assertFalse { Emojis.isEmoji("1") }
        assertFalse { Emojis.isEmoji("#") }
        assertFalse { Emojis.isEmoji("*") }
        assertFalse { Emojis.containsEmoji("Call 0800 123") }
        assertFalse { Emojis.onlyContainsEmojis("123") }
        assertEquals(0, Emojis.countEmojis("Order #1 costs 20 € *now*"))
    }

    @Test
    fun keepsPlainDigitsAndSymbolsWhenRemovingEmojis() {
        assertEquals("Version 2.0 ", Emojis.removeEmojis("Version 2.0 🎉"))
        assertEquals("Order #1 costs 20 € *now*", Emojis.removeEmojis("Order #1 costs 20 € *now*"))
        assertEquals(" first", Emojis.removeEmojis("1️⃣ first"))
    }

    @Test
    fun detectsEmojisOfRecentUnicodeVersions() {
        // Added in Unicode / Emoji 17.0, after the former emoji table was last generated. Detection follows the
        // Unicode version of the running JDK, so the test is skipped on a JDK which doesn't know these code points yet.
        Assumptions.assumeTrue(Character.isDefined(0x1FACD), "The running JDK does not support Unicode 17.0")
        assertTrue { Emojis.isEmoji("🫍") }
        assertTrue { Emojis.isEmoji("🪊") }
        assertEquals("Orca ", Emojis.removeEmojis("Orca 🫍"))
    }

    @Test
    fun treatsDigitsWithCombiningMarksAsText() {
        assertFalse { Emojis.isEmoji("1\u0301") }
        assertFalse { Emojis.containsEmoji("1\u0301") }
    }

    @Test
    fun keepsStraySkinToneModifiersWithTheirBaseCharacter() {
        // A skin tone modifier after a regular letter is a malformed sequence and belongs to that letter
        assertFalse { Emojis.isEmoji("x🏾") }
        assertEquals("x🏾", Emojis.removeEmojis("x🏾"))
    }

    @Test
    fun treatsTextPresentationSequencesAsText() {
        // U+FE0E (variation selector 15) requests text instead of emoji presentation, U+FE0F requests an emoji
        assertFalse { Emojis.isEmoji("\u2764\uFE0E") }
        assertFalse { Emojis.isEmoji("😀\uFE0E") }
        assertFalse { Emojis.onlyContainsEmojis("\u2194\uFE0E") }
        assertEquals("I \u2764\uFE0E it", Emojis.removeEmojis("I \u2764\uFE0E it"))
        assertTrue { Emojis.isEmoji("\u2764\uFE0F") }
    }
}
