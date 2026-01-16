/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import sirius.kernel.health.Exceptions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * Provides various methods to clean-up or reduce strings.
 * <p>
 * Please note, that most of the functions provided here are intended to be used via {@code Strings.cleanup(..)} and
 * therefore provide no internal <tt>null</tt> check.
 *
 * @see Strings#cleanup(String, Iterable)
 */
public class StringCleanup {

    private static final Pattern PATTERN_CONTROL_CHARACTERS = Pattern.compile("\\p{Cntrl}");
    private static final String REGEX_WHITESPACE = "[\\s\\p{Z}]";
    private static final Pattern PATTERN_WHITESPACE = Pattern.compile(REGEX_WHITESPACE);
    private static final Pattern PATTERN_WHITESPACES = Pattern.compile(REGEX_WHITESPACE + "+");
    private static final Pattern PATTERN_PUNCTUATION = Pattern.compile("\\p{Punct}");
    private static final Pattern PATTERN_NON_ALPHA_NUMERIC = Pattern.compile("([^\\p{L}\\d])");
    private static final Pattern PATTERN_NON_LETTER = Pattern.compile("\\P{L}");
    private static final Pattern PATTERN_NON_DIGIT = Pattern.compile("\\D");
    private static final Pattern PATTERN_DECIMAL_ENTITY = Pattern.compile("&#(\\d+);");
    private static final Pattern PATTERN_HEX_ENTITY = Pattern.compile("&#x([0-9a-fA-F]+);");
    private static final Pattern PATTERN_BR_TAG = Pattern.compile("<(br|BR) *+/? *+>");
    private static final Pattern PATTERN_LILI_TAG = Pattern.compile("<(/li|/LI)>\\r?\\n?\\t?<(li|LI)>");
    private static final Pattern PATTERN_LI_TAG = Pattern.compile("<(/?li|/?LI)>");
    private static final Pattern PATTERN_PP_TAG = Pattern.compile("<(/p|/P)>\\r?\\n?\\t?<([pP])>");
    private static final Pattern PATTERN_P_TAG = Pattern.compile("<(/?p|/?P)>");

    /**
     * Holds the name of the {@code <p>} tag.
     */
    public static final String TAG_P = "p";
    /**
     * Holds the name of the {@code <br>} tag.
     */
    public static final String TAG_BR = "br";
    /**
     * Holds the name of the {@code <div>} tag.
     */
    public static final String TAG_DIV = "div";
    /**
     * Holds the name of the {@code <span>} tag.
     */
    public static final String TAG_SPAN = "span";
    /**
     * Holds the name of the {@code <small>} tag.
     */
    public static final String TAG_SMALL = "small";
    /**
     * Holds the name of the {@code <h1>} tag.
     */
    public static final String TAG_H1 = "h1";
    /**
     * Holds the name of the {@code <h2>} tag.
     */
    public static final String TAG_H2 = "h2";
    /**
     * Holds the name of the {@code <h3>} tag.
     */
    public static final String TAG_H3 = "h3";
    /**
     * Holds the name of the {@code <h4>} tag.
     */
    public static final String TAG_H4 = "h4";
    /**
     * Holds the name of the {@code <h5>} tag.
     */
    public static final String TAG_H5 = "h5";
    /**
     * Holds the name of the {@code <h6>} tag.
     */
    public static final String TAG_H6 = "h6";
    /**
     * Holds the name of the {@code <b>} tag.
     */
    public static final String TAG_B = "b";
    /**
     * Holds the name of the {@code <strong>} tag.
     */
    public static final String TAG_STRONG = "strong";
    /**
     * Holds the name of the {@code <i>} tag.
     */
    public static final String TAG_I = "i";
    /**
     * Holds the name of the {@code <em>} tag.
     */
    public static final String TAG_EM = "em";
    /**
     * Holds the name of the {@code <u>} tag.
     */
    public static final String TAG_U = "u";
    /**
     * Holds the name of the {@code <sup>} tag.
     */
    public static final String TAG_SUP = "sup";
    /**
     * Holds the name of the {@code <sub>} tag.
     */
    public static final String TAG_SUB = "sub";
    /**
     * Holds the name of the {@code <mark>} tag.
     */
    public static final String TAG_MARK = "mark";
    /**
     * Holds the name of the {@code <hr>} tag.
     */
    public static final String TAG_HR = "hr";
    /**
     * Holds the name of the {@code <dl>} tag.
     */
    public static final String TAG_DL = "dl";
    /**
     * Holds the name of the {@code <dt>} tag.
     */
    public static final String TAG_DT = "dt";
    /**
     * Holds the name of the {@code <dd>} tag.
     */
    public static final String TAG_DD = "dd";
    /**
     * Holds the name of the {@code <ol>} tag.
     */
    public static final String TAG_OL = "ol";
    /**
     * Holds the name of the {@code <ul>} tag.
     */
    public static final String TAG_UL = "ul";
    /**
     * Holds the name of the {@code <li>} tag.
     */
    public static final String TAG_LI = "li";

    /**
     * Holds a list of all allowed HTML tag names.
     */
    public static final List<String> ALLOWED_HTML_TAG_NAMES = List.of(TAG_P,
                                                                      TAG_BR,
                                                                      TAG_DIV,
                                                                      TAG_SPAN,
                                                                      TAG_SMALL,
                                                                      TAG_H1,
                                                                      TAG_H2,
                                                                      TAG_H3,
                                                                      TAG_H4,
                                                                      TAG_H5,
                                                                      TAG_H6,
                                                                      TAG_B,
                                                                      TAG_STRONG,
                                                                      TAG_I,
                                                                      TAG_EM,
                                                                      TAG_U,
                                                                      TAG_SUP,
                                                                      TAG_SUB,
                                                                      TAG_MARK,
                                                                      TAG_HR,
                                                                      TAG_DL,
                                                                      TAG_DT,
                                                                      TAG_DD,
                                                                      TAG_OL,
                                                                      TAG_UL,
                                                                      TAG_LI);

    private static final Pattern PATTERN_STRIP_XML = Pattern.compile("\\s*" + Strings.REGEX_DETECT_XML_TAGS + "\\s*");
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
     * Capitalizes the first character of the given string.
     *
     * @param input the input to process
     * @return the resulting string
     */
    public static String capitalize(@Nonnull String input) {
        char titleCasedChar = Character.toTitleCase(input.charAt(0));
        if (titleCasedChar == input.charAt(0)) {
            return input;
        }
        return titleCasedChar + input.substring(1);
    }

    /**
     * Removes all {@linkplain #PATTERN_STRIP_XML XML tags} from the given string.
     *
     * @param input the input to process
     * @return the resulting string
     */
    @Nonnull
    public static String removeXml(@Nonnull String input) {
        return PATTERN_STRIP_XML.matcher(input).replaceAll("");
    }

    /**
     * Resolves encoded HTML entities to their plain text equivalent in the given string.
     *
     * @param input the input to process
     * @return the resulting string
     */
    @Nonnull
    public static String decodeHtmlEntities(@Nonnull String input) {
        input = input.replace("&nbsp;", " ")
                     .replace("&auml;", "ä")
                     .replace("&ouml;", "ö")
                     .replace("&uuml;", "ü")
                     .replace("&Auml;", "Ä")
                     .replace("&Ouml;", "Ö")
                     .replace("&Uuml;", "Ü")
                     .replace("&szlig;", "ß")
                     .replace("&lt;", "<")
                     .replace("&gt;", ">")
                     .replace("&quot;", "\"")
                     .replace("&apos;", "'")
                     .replace("&amp;", "&")
                     .replace("&#8226; ", "* ")
                     .replace("&#8226;", "* ")
                     .replace("&#8227; ", "* ")
                     .replace("&#8227;", "* ")
                     .replace("&#8259; ", "* ")
                     .replace("&#8259;", "* ");
        input = Strings.replaceAll(PATTERN_DECIMAL_ENTITY, input, s -> {
            try {
                return String.valueOf(Character.toChars(Integer.parseInt(s)));
            } catch (NumberFormatException e) {
                Exceptions.ignore(e);
            } catch (Exception e) {
                Exceptions.handle(e);
            }
            return "";
        });
        input = Strings.replaceAll(PATTERN_HEX_ENTITY, input, s -> {
            try {
                return String.valueOf(Character.toChars(Integer.parseInt(s, 16)));
            } catch (NumberFormatException e) {
                Exceptions.ignore(e);
            } catch (Exception e) {
                Exceptions.handle(e);
            }
            return "";
        });

        return input;
    }

    /**
     * Normalizes a text by removing all HTML, entities and special characters.
     *
     * @param input the input to process
     * @return the resulting string
     */
    @Nonnull
    public static String htmlToPlainText(@Nonnull String input) {
        String normalizedText = input;

        if (PATTERN_STRIP_XML.matcher(normalizedText).find()) {
            // Reduce all contained whitespaces, tabs, and line breaks
            normalizedText = Strings.cleanup(normalizedText, StringCleanup::reduceWhitespace);
            // Replace br tags with line breaks
            normalizedText = PATTERN_BR_TAG.matcher(normalizedText).replaceAll("\n");
            // Replace li tags with line breaks
            normalizedText = PATTERN_LILI_TAG.matcher(normalizedText).replaceAll("\n");
            normalizedText = PATTERN_LI_TAG.matcher(normalizedText).replaceAll("\n");
            // Replace p tags with line breaks
            normalizedText = PATTERN_PP_TAG.matcher(normalizedText).replaceAll("\n");
            normalizedText = PATTERN_P_TAG.matcher(normalizedText).replaceAll("\n");

            // Iterates the lines to clean them up properly, preserving the line breaks converted above,
            // as the RegEx used by removeXml would detect and clean them.
            StringBuilder builder = new StringBuilder();
            normalizedText.lines().forEach(lineText -> {
                if (!builder.isEmpty()) {
                    builder.append("\n");
                }

                // Remove any other tags
                String normalizedLine = Strings.cleanup(lineText, StringCleanup::removeXml);
                // Decode entities
                normalizedLine = Strings.cleanup(normalizedLine, StringCleanup::decodeHtmlEntities);
                builder.append(normalizedLine);
            });
            return builder.toString();
        }

        return normalizedText;
    }

    /**
     * Removes all umlauts and other decorated latin characters.
     *
     * @param term the term to reduce characters in
     * @return the term with all decorated latin characters replaced
     */
    public static String reduceCharacters(@Nullable String term) {
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
    public static String replaceXml(@Nullable String input) {
        if (Strings.isEmpty(input)) {
            return input;
        }

        String alreadyStrippedContent = input;
        String contentToStrip;
        do {
            contentToStrip = alreadyStrippedContent;
            alreadyStrippedContent = PATTERN_STRIP_XML.matcher(contentToStrip).replaceFirst(" ");
        } while (!Strings.areEqual(contentToStrip, alreadyStrippedContent));

        return alreadyStrippedContent;
    }

    /**
     * Escapes XML characters so that the given string can be safely embedded in XML.
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
    @Nullable
    public static String nlToBr(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("\n", " <br> ");
    }
}
