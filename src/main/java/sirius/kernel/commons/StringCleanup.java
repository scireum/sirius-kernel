/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class StringCleanup {

    public static final Pattern PATTERN_CONTROL_CHARACTERS = Pattern.compile("\\p{Cntrl}");
    public static final Pattern PATTERN_WHITESPACE = Pattern.compile("\\s");
    public static final Pattern PATTERN_WHITESPACES = Pattern.compile("\\s+");
    public static final Pattern PATTERN_PUNCTUATION = Pattern.compile("\\p{Punct}");
    public static final Pattern PATTERN_NON_ALPHA_NUMERIC = Pattern.compile("([^\\p{L}\\d])");
    public static final Pattern PATTERN_NON_LETTER = Pattern.compile("\\P{L}");
    public static final Pattern PATTERN_NON_DIGIT = Pattern.compile("\\D");

    private static final Map<Integer, String> unicodeMapping = new TreeMap<>();

    static {
        translateRange(0x00C0, "A", "A", "A", "A", "AE", "A", "AE", "C", "E", "E", "E", "E", "I", "I", "I", "I");
        translateRange(0x00D0, "D", "N", "O", "O", "O", "O", "OE", null, null, "U", "U", "U", "UE", "Y", null, "ss");
        translateRange(0x00E0, "a", "a", "a", "a", "ae", "a", "ae", "c", "e", "e", "e", "e", "i", "i", "i", "i");
        translateRange(0x00F0, null, "n", "o", "o", "o", "o", "oe", null, null, "u", "u", "u", "ue", "y", null, "y");
        translateRange(0x0130, null, null, "IJ", "ij", "J", "j", "K", "k", "k", "L", "l", "L", "l", "L", "l", "L");
        translateRange(0xFB00,
                       "ff",
                       "fi",
                       "fl",
                       "ffi",
                       "ffl",
                       "ft",
                       "st",
                       null,
                       null,
                       null,
                       null,
                       null,
                       null,
                       null,
                       null,
                       null);
    }

    private StringCleanup() {
        // This class only provides static utility methods...
    }

    private static void translateRange(int offset, String... replacements) {
        int index = offset;
        for (String replacement : replacements) {
            if (replacement != null) {
                unicodeMapping.put(index, replacement);
            }

            index++;
        }
    }

    @Nonnull
    public static String removeControlCharacters(@Nonnull String input) {
        return PATTERN_CONTROL_CHARACTERS.matcher(input).replaceAll("");
    }

    @Nonnull
    public static String replaceControlCharacters(@Nonnull String input) {
        return PATTERN_CONTROL_CHARACTERS.matcher(input).replaceAll(" ");
    }

    @Nonnull
    public static String removePunctuation(@Nonnull String input) {
        return PATTERN_PUNCTUATION.matcher(input).replaceAll("");
    }

    @Nonnull
    public static String replacePunctuation(@Nonnull String input) {
        return PATTERN_PUNCTUATION.matcher(input).replaceAll(" ");
    }

    @Nonnull
    public static String removeNonAlphaNumeric(@Nonnull String input) {
        return PATTERN_NON_ALPHA_NUMERIC.matcher(input).replaceAll("");
    }

    @Nonnull
    public static String replaceNonAlphaNumeric(@Nonnull String input) {
        return PATTERN_NON_ALPHA_NUMERIC.matcher(input).replaceAll(" ");
    }

    @Nonnull
    public static String removeNonLetter(@Nonnull String input) {
        return PATTERN_NON_LETTER.matcher(input).replaceAll("");
    }

    @Nonnull
    public static String replaceNonLetter(@Nonnull String input) {
        return PATTERN_NON_LETTER.matcher(input).replaceAll(" ");
    }

    @Nonnull
    public static String removeNonDigit(@Nonnull String input) {
        return PATTERN_NON_DIGIT.matcher(input).replaceAll("");
    }

    @Nonnull
    public static String replaceNonDigit(@Nonnull String input) {
        return PATTERN_NON_DIGIT.matcher(input).replaceAll(" ");
    }

    @Nonnull
    public static String removeWhitespace(@Nonnull String input) {
        return PATTERN_WHITESPACE.matcher(input).replaceAll("");
    }

    @Nonnull
    public static String reduceWhitespace(@Nonnull String input) {
        return PATTERN_WHITESPACES.matcher(input).replaceAll(" ");
    }

    @Nonnull
    public static String trim(@Nonnull String input) {
        return input.trim();
    }

    @Nonnull
    public static String lowercase(@Nonnull String input) {
        return input.toLowerCase();
    }

    @Nonnull
    public static String uppercase(@Nonnull String input) {
        return input.toUpperCase();
    }

    /**
     * Removes all umlauts and other decorated latin characters.
     *
     * @param term the term to reduce characters in
     * @return the term with all decorated latin characters replaced
     */
    public static String reduceCharacters(String term) {
        if (Strings.isEmpty(term)) {
            return term;
        }

        StringBuilder result = null;

        for (int i = 0; i < term.length(); ++i) {
            String replacement = unicodeMapping.get(term.codePointAt(i));
            if (replacement == null) {
                if (result != null) {
                    result.append(term.charAt(i));
                }
            } else {
                if (result == null) {
                    result = new StringBuilder().append(term, 0, i);
                }
                result.append(replacement);
            }
        }

        return result == null ? term : result.toString();
    }
}
