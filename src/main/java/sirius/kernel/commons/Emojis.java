/*
 * Made with all the love in the world
 * by scireum in Stuttgart, Germany
 *
 * Copyright by scireum GmbH
 * https://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Provides helper methods to work with emojis.
 * <p>
 * Text is split into extended grapheme clusters (user-perceived characters) as defined by
 * <a href="https://www.unicode.org/reports/tr29/">Unicode Standard Annex #29</a>, so that sequences like
 * 👩🏾‍🚀 (woman astronaut with skin tone) or 🇫🇷 (flag) are treated as one emoji. Whether a cluster is an emoji is
 * decided by the Unicode emoji properties of the running JDK, which keeps up with new Unicode versions without any
 * further maintenance.
 */
public class Emojis {

    /**
     * Matches a single extended grapheme cluster.
     */
    private static final Pattern GRAPHEME_CLUSTER = Pattern.compile("\\X");

    private static final int REGIONAL_INDICATOR_A = 0x1F1E6;
    private static final int REGIONAL_INDICATOR_Z = 0x1F1FF;
    private static final int COMBINING_KEYCAP = 0x20E3;

    /**
     * Variation selector 15, which requests a character like ❤ to be displayed as text rather than as an emoji.
     */
    private static final int TEXT_PRESENTATION_SELECTOR = 0xFE0E;

    private Emojis() {
    }

    /**
     * Checks whether the given {@code string} contains a single emoji.
     * <p>
     * Use {@link #onlyContainsEmojis(String)} to check if a string consists only of (multiple) emojis.
     *
     * @param string the string to check
     * @return <tt>true</tt> if the string contains a single emoji, <tt>false</tt> otherwise
     * @see #onlyContainsEmojis(String)
     */
    public static boolean isEmoji(String string) {
        if (Strings.isEmpty(string)) {
            return false;
        }

        Matcher matcher = GRAPHEME_CLUSTER.matcher(string);
        return matcher.find() && matcher.end() == string.length() && isEmojiCluster(matcher.group());
    }

    /**
     * Checks whether the given {@code string} contains at least one emoji.
     *
     * @param string the string to check
     * @return <tt>true</tt> if the string contains at least one emoji, <tt>false</tt> otherwise
     */
    public static boolean containsEmoji(String string) {
        if (Strings.isEmpty(string)) {
            return false;
        }

        return clusters(string).anyMatch(Emojis::isEmojiCluster);
    }

    /**
     * Checks whether the given {@code string} contains only emojis.
     * <p>
     * Use {@link #isEmoji(String)} to check if a string contains a single emoji.
     *
     * @param string the string to check
     * @return <tt>true</tt> if the string contains only emojis, <tt>false</tt> otherwise
     * @see #isEmoji(String)
     * @see #onlyContainsEmojisWithWhitespace(String)
     */
    public static boolean onlyContainsEmojis(String string) {
        if (Strings.isEmpty(string)) {
            return false;
        }

        return clusters(string).allMatch(Emojis::isEmojiCluster);
    }

    /**
     * Checks whether the given {@code string} contains only emojis with additional whitespace characters.
     * <p>
     * Note that pure whitespace strings will return <tt>false</tt>.
     *
     * @param string the string to check
     * @return <tt>true</tt> if the string contains only emojis and whitespace, <tt>false</tt> otherwise
     * @see #onlyContainsEmojis(String)
     */
    public static boolean onlyContainsEmojisWithWhitespace(String string) {
        return onlyContainsEmojis(string.replaceAll("\\s", ""));
    }

    /**
     * Counts the emojis contained in the given {@code string}.
     *
     * @param string the string to check
     * @return the number of emojis contained in the string
     */
    public static int countEmojis(String string) {
        return (int) clusters(string).filter(Emojis::isEmojiCluster).count();
    }

    /**
     * Removes all emojis from the given {@code string}.
     *
     * @param string the string to remove the emojis from
     * @return the string without emojis
     */
    public static String removeEmojis(String string) {
        StringBuilder builder = new StringBuilder();
        clusters(string).filter(cluster -> !isEmojiCluster(cluster)).forEach(builder::append);
        return builder.toString();
    }

    private static Stream<String> clusters(String string) {
        return GRAPHEME_CLUSTER.matcher(string).results().map(MatchResult::group);
    }

    /**
     * Determines whether the given grapheme cluster represents an emoji.
     * <p>
     * A flag consists of exactly two regional indicators. Any other cluster is an emoji if it starts with a code point
     * carrying the Unicode <tt>Emoji</tt> property. The exception are the keycap bases ("0" to "9", "#" and "*"), which
     * carry that property as well but only form an emoji in combination with the combining enclosing keycap (U+20E3).
     * A cluster containing the text presentation selector (U+FE0E) explicitly asks to be shown as text, as per
     * <a href="https://www.unicode.org/reports/tr51/">UTS #51</a>, and is therefore not considered an emoji.
     *
     * @param cluster a single extended grapheme cluster
     * @return <tt>true</tt> if the cluster is an emoji, <tt>false</tt> otherwise
     */
    private static boolean isEmojiCluster(String cluster) {
        int first = cluster.codePointAt(0);
        if (isRegionalIndicator(first)) {
            return cluster.codePointCount(0, cluster.length()) == 2
                   && isRegionalIndicator(cluster.codePointAt(Character.charCount(first)));
        }
        if (!Character.isEmoji(first) || cluster.indexOf(TEXT_PRESENTATION_SELECTOR) >= 0) {
            return false;
        }

        return !isKeycapBase(first) || cluster.indexOf(COMBINING_KEYCAP) >= 0;
    }

    private static boolean isRegionalIndicator(int codePoint) {
        return codePoint >= REGIONAL_INDICATOR_A && codePoint <= REGIONAL_INDICATOR_Z;
    }

    private static boolean isKeycapBase(int codePoint) {
        return codePoint == '#' || codePoint == '*' || (codePoint >= '0' && codePoint <= '9');
    }
}
