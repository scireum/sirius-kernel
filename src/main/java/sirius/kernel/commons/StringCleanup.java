/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * Provides various methods to clean-up or reduce strings.
 *
 * @see Strings#cleanup(String, Iterable)
 */
public class StringCleanup {

    private static final Pattern PATTERN_CONTROL_CHARACTERS = Pattern.compile("\\p{Cntrl}");
    private static final Pattern PATTERN_WHITESPACE = Pattern.compile("\\s");
    private static final Pattern PATTERN_WHITESPACES = Pattern.compile("\\s+");
    private static final Pattern PATTERN_PUNCTUATION = Pattern.compile("\\p{Punct}");
    private static final Pattern PATTERN_NON_ALPHA_NUMERIC = Pattern.compile("([^\\p{L}\\d])");
    private static final Pattern PATTERN_NON_LETTER = Pattern.compile("\\P{L}");
    private static final Pattern PATTERN_NON_DIGIT = Pattern.compile("\\D");
    private static final Pattern STRIP_XML_REGEX = Pattern.compile("\\s*</?[a-zA-Z0-9]+[^>]*>\\s*");

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

    /**
     * Removes all control characters from the given string.
     *
     * @param input the input to process
     * @return the resulting string
     */
    @Nonnull
    public static String removeControlCharacters(@Nonnull String input) {
        return PATTERN_CONTROL_CHARACTERS.matcher(input).replaceAll("");
    }

    /**
     * Replaces all control characters with a whitespace character.
     * <p>
     * Most probably this should be followed by {@link #reduceWhitespace(String)}.
     *
     * @param input the input to process
     * @return the resulting string
     */
    @Nonnull
    public static String replaceControlCharacters(@Nonnull String input) {
        return PATTERN_CONTROL_CHARACTERS.matcher(input).replaceAll(" ");
    }

    /**
     * Creates a replacement function which replaces all control characters with the given replacement.
     *
     * @param replacement the replacement to use for control characters
     * @return a function which replaces all control characters by the replacement
     */
    @Nonnull
    public static UnaryOperator<String> replaceControlCharactersWith(@Nonnull String replacement) {
        return input -> PATTERN_CONTROL_CHARACTERS.matcher(input).replaceAll(replacement);
    }

    /**
     * Removes all punctuation characters from the given string.
     *
     * @param input the input to process
     * @return the resulting string
     */
    @Nonnull
    public static String removePunctuation(@Nonnull String input) {
        return PATTERN_PUNCTUATION.matcher(input).replaceAll("");
    }

    /**
     * Replaces all punctuation characters with a whitespace character.
     * <p>
     * Most probably this should be followed by {@link #reduceWhitespace(String)}.
     *
     * @param input the input to process
     * @return the resulting string
     */
    @Nonnull
    public static String replacePunctuation(@Nonnull String input) {
        return PATTERN_PUNCTUATION.matcher(input).replaceAll(" ");
    }

    /**
     * Creates a replacement function which replaces all punctuation characters with the given replacement.
     *
     * @param replacement the replacement to use for control characters
     * @return a function which replaces all punctuation characters by the replacement
     */
    @Nonnull
    public static UnaryOperator<String> replacePunctuationCharactersWith(@Nonnull String replacement) {
        return input -> PATTERN_PUNCTUATION.matcher(input).replaceAll(replacement);
    }

    /**
     * Removes all non-alphanumeric (0-9 a-z A-Z) characters from the given string.
     *
     * @param input the input to process
     * @return the resulting string
     */
    @Nonnull
    public static String removeNonAlphaNumeric(@Nonnull String input) {
        return PATTERN_NON_ALPHA_NUMERIC.matcher(input).replaceAll("");
    }

    /**
     * Replaces all non-alphanumeric characters with a whitespace character.
     * <p>
     * Most probably this should be followed by {@link #reduceWhitespace(String)}.
     *
     * @param input the input to process
     * @return the resulting string
     */
    @Nonnull
    public static String replaceNonAlphaNumeric(@Nonnull String input) {
        return PATTERN_NON_ALPHA_NUMERIC.matcher(input).replaceAll(" ");
    }

    /**
     * Creates a replacement function which replaces all non-alphanumeric characters with the given replacement.
     *
     * @param replacement the replacement to use for control characters
     * @return a function which replaces all non-alphanumeric characters by the replacement
     */
    @Nonnull
    public static UnaryOperator<String> replaceNonAlphaNumericWith(@Nonnull String replacement) {
        return input -> PATTERN_NON_ALPHA_NUMERIC.matcher(input).replaceAll(replacement);
    }

    /**
     * Removes all non-letter (<tt>\P{L}</tt>) characters from the given string.
     *
     * @param input the input to process
     * @return the resulting string
     */
    @Nonnull
    public static String removeNonLetter(@Nonnull String input) {
        return PATTERN_NON_LETTER.matcher(input).replaceAll("");
    }

    /**
     * Replaces all non-letter (<tt>\P{L}</tt>) characters with a whitespace character.
     * <p>
     * Most probably this should be followed by {@link #reduceWhitespace(String)}.
     *
     * @param input the input to process
     * @return the resulting string
     */
    @Nonnull
    public static String replaceNonLetter(@Nonnull String input) {
        return PATTERN_NON_LETTER.matcher(input).replaceAll(" ");
    }

    /**
     * Creates a replacement function which replaces all non-letter (<tt>\P{L}</tt>) characters with the given
     * replacement.
     *
     * @param replacement the replacement to use for control characters
     * @return a function which replaces all non-letter characters by the replacement
     */
    @Nonnull
    public static UnaryOperator<String> replaceNonLetterWith(@Nonnull String replacement) {
        return input -> PATTERN_NON_LETTER.matcher(input).replaceAll(replacement);
    }

    /**
     * Removes all non-digit (<tt>\D</tt>) characters from the given string.
     *
     * @param input the input to process
     * @return the resulting string
     */
    @Nonnull
    public static String removeNonDigit(@Nonnull String input) {
        return PATTERN_NON_DIGIT.matcher(input).replaceAll("");
    }

    /**
     * Replaces all non-digit (<tt>\D</tt>) characters with a whitespace character.
     * <p>
     * Most probably this should be followed by {@link #reduceWhitespace(String)}.
     *
     * @param input the input to process
     * @return the resulting string
     */
    @Nonnull
    public static String replaceNonDigit(@Nonnull String input) {
        return PATTERN_NON_DIGIT.matcher(input).replaceAll(" ");
    }

    /**
     * Creates a replacement function which replaces all non-digit (<tt>\D</tt>) characters with the given
     * replacement.
     *
     * @param replacement the replacement to use for control characters
     * @return a function which replaces all non-digit characters by the replacement
     */
    @Nonnull
    public static UnaryOperator<String> replaceNonDigitWith(@Nonnull String replacement) {
        return input -> PATTERN_NON_DIGIT.matcher(input).replaceAll(replacement);
    }

    /**
     * Removes all whitespace characters from the given string.
     *
     * @param input the input to process
     * @return the resulting string
     */
    @Nonnull
    public static String removeWhitespace(@Nonnull String input) {
        return PATTERN_WHITESPACE.matcher(input).replaceAll("");
    }

    /**
     * Replaces multiple whitespace characters with a single whitespace character (" ").
     *
     * @param input the input to process
     * @return the resulting string
     */
    @Nonnull
    public static String reduceWhitespace(@Nonnull String input) {
        return PATTERN_WHITESPACES.matcher(input).replaceAll(" ");
    }

    /**
     * Trims the given string.
     * <p>
     * This is essentially an alias for {@code String::trim}.
     *
     * @param input the input to process
     * @return the resulting string
     */
    @Nonnull
    public static String trim(@Nonnull String input) {
        return input.trim();
    }

    /**
     * Converts the string to lowercase.
     * <p>
     * This is essentially an alias for {@code String::lowercase}.
     *
     * @param input the input to process
     * @return the resulting string
     */
    @Nonnull
    public static String lowercase(@Nonnull String input) {
        return input.toLowerCase();
    }

    /**
     * Converts the string to uppercase.
     * <p>
     * This is essentially an alias for {@code String::uppercase}.
     *
     * @param input the input to process
     * @return the resulting string
     */
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

    /**
     * Replaces XML tags by a single whitespace character.
     * <p>
     * Most probably this should be followed by {@link #reduceWhitespace(String)} and also
     * {@link StringCleanup#trim(String)}
     *
     * @param input the input to process
     * @return the resulting string
     */
    public static String replaceXml(String input) {
        if (Strings.isEmpty(input)) {
            return input;
        }

        String alreadyStrippedContent = input;
        String contentToStrip;
        do {
            contentToStrip = alreadyStrippedContent;
            alreadyStrippedContent = STRIP_XML_REGEX.matcher(contentToStrip).replaceFirst(" ");
        } while (!Strings.areEqual(contentToStrip, alreadyStrippedContent));

        return alreadyStrippedContent;
    }

    /**
     * Escapes XML characters to that the given string can be safely embedded in XML.
     *
     * @param input the input to process
     * @return the resulting string
     */
    public static String escapeXml(@Nullable String input) {
        if (Strings.isEmpty(input)) {
            return "";
        }

        final StringBuilder result = new StringBuilder();
        final StringCharacterIterator iterator = new StringCharacterIterator(input);
        char character = iterator.current();
        while (character != CharacterIterator.DONE) {
            if (character == '<') {
                result.append("&lt;");
            } else if (character == '>') {
                result.append("&gt;");
            } else if (character == '\"') {
                result.append("&quot;");
            } else if (character == '\'') {
                result.append("&#039;");
            } else if (character == '&') {
                result.append("&amp;");
            } else {
                // the char is not a special one
                // add it to the result as is
                result.append(character);
            }
            character = iterator.next();
        }

        return result.toString();
    }

    /**
     * Provides a very simplistic approach to convert newlines to HTML line breaks.
     * <p>
     * Note that most modern browsers will probably be better off by using a CSS "whitespace" setting, but some
     * old html renderers need raw br tags to properly render.
     *
     * @param input the input to process
     * @return the resulting string
     */
    public static String nlToBr(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("\n", " <br> ");
    }
}
