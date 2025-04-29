/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import sirius.kernel.nls.NLS;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides various helper methods for dealing with Java <tt>Strings</tt>
 * <p>
 * The {@link Value} class provides some additional methods for working with nullable strings like
 * {@link Value#left(int)}, {@link Value#toLowerCase()} etc.
 * <p>
 * This class can and should not be instantiated, as all methods are static.
 *
 * @see Value
 */
public class Strings {

    /**
     * Contains the characters which should be used in order to depict an ellipsis.
     */
    private static final String ELLIPSIS = "…";

    /**
     * Contains the marker/signal which should be used to depict that the string has been truncated.
     */
    private static final String TRUNCATED_SIGNAL = ELLIPSIS + "[" + ELLIPSIS + "]" + ELLIPSIS;

    /**
     * Contains all characters which can safely be used for codes without too much confusion (e.g. 0 vs O are
     * excluded).
     */
    private static final char[] VALID_CODE_CHARS = {'1',
                                                    '2',
                                                    '3',
                                                    '4',
                                                    '5',
                                                    '6',
                                                    '7',
                                                    '8',
                                                    '9',
                                                    'a',
                                                    'b',
                                                    'c',
                                                    'd',
                                                    'e',
                                                    'f',
                                                    'g',
                                                    'h',
                                                    'i',
                                                    'j',
                                                    'k',
                                                    'm',
                                                    'n',
                                                    'p',
                                                    'q',
                                                    'r',
                                                    's',
                                                    't',
                                                    'u',
                                                    'v',
                                                    'w',
                                                    'z'};

    /*
     * All methods are static, therefore no instances need to be created.
     */
    private Strings() {
    }

    /**
     * Checks if the string representation of the given object is "" or <tt>null</tt>.
     *
     * @param string the object which is to be checked
     * @return <tt>true</tt> if string is <tt>null</tt> or "", <tt>false</tt> otherwise
     */
    public static boolean isEmpty(@Nullable Object string) {
        if (string == null) {
            return true;
        }
        return string.toString() == null || string.toString().isEmpty();
    }

    /**
     * Checks if the string representation of the given object is neither "" nor <tt>null</tt>.
     *
     * @param string the object which is to be checked
     * @return <tt>true</tt> if string is not <tt>null</tt> and not "", <tt>false</tt> otherwise
     */
    public static boolean isFilled(@Nullable Object string) {
        if (string == null) {
            return false;
        }
        return string.toString() != null && !string.toString().isEmpty();
    }

    /**
     * Checks if the string representations of the given objects are all "" or <tt>null</tt>.
     *
     * @param first   the first object which is to be checked
     * @param second  the second object which is to be checked
     * @param further additional objects to be checked
     * @return <tt>true</tt> if all strings are <tt>null</tt> or "", <tt>false</tt> if one of them is filled
     * @see #isEmpty(Object)
     */
    public static boolean areAllEmpty(Object first, Object second, Object... further) {
        if (Strings.isFilled(first) || Strings.isFilled(second)) {
            return false;
        }
        if (further != null) {
            return Stream.of(further).allMatch(Strings::isEmpty);
        }
        return true;
    }

    /**
     * Checks if the string representations of the given objects are not "" or <tt>null</tt>.
     *
     * @param first   the first object which is to be checked
     * @param second  the second object which is to be checked
     * @param further additional objects to be checked
     * @return <tt>true</tt> if all strings are not <tt>null</tt> or "", <tt>false</tt> if one of them is empty
     * @see #isFilled(Object)
     */
    public static boolean areAllFilled(Object first, Object second, Object... further) {
        if (Strings.isEmpty(first) || Strings.isEmpty(second)) {
            return false;
        }
        if (further != null) {
            return Stream.of(further).allMatch(Strings::isFilled);
        }
        return true;
    }

    /**
     * Compares the given <tt>Strings</tt> while treating upper- and lowercase characters as equal.
     * <p>
     * This is essentially the same as {@code left.equalsIgnoreCase(right)}
     * while gracefully handling <tt>null</tt> values.
     *
     * @param left  the first string to be compared
     * @param right the second string to be compared with
     * @return <tt>true</tt> if both values are empty or if both strings are equal
     * while ignoring their case - <tt>false</tt> otherwise
     */
    public static boolean equalIgnoreCase(@Nullable String left, @Nullable String right) {
        // Implementation node: This doesn't use areEqual with toLowerCase as modified
        // as the implementation of String.equalsIgnoreCase is more efficient.
        if (isEmpty(left)) {
            return isEmpty(right);
        }
        return left.equalsIgnoreCase(right);
    }

    /**
     * Compares the given <tt>Strings</tt> just like {@link String#compareTo(String)}
     * but with graceful handling for <tt>null</tt> values.
     *
     * @param left     the first string to be compared
     * @param right    the second string to be compared with
     * @param modifier the modifier function to be applied on both arguments. This has to handle <tt>null</tt> values
     *                 correctly
     * @return <tt>true</tt> if both values are empty or if both strings are equal - <tt>false</tt> otherwise
     */
    public static boolean areEqual(@Nullable Object left, @Nullable Object right, UnaryOperator<Object> modifier) {
        Object effectiveLeft = modifier.apply(left);
        Object effectiveRight = modifier.apply(right);
        if (isEmpty(effectiveLeft)) {
            return isEmpty(effectiveRight);
        }
        return Objects.equals(effectiveLeft, effectiveRight);
    }

    /**
     * Compares the given <tt>Strings</tt> just like {@link String#compareTo(String)}
     * but with graceful handling for <tt>null</tt> values.
     *
     * @param left  the first string to be compared
     * @param right the second string to be compared with
     * @return <tt>true</tt> if both values are empty or if both strings are equal - <tt>false</tt> otherwise
     */
    public static boolean areEqual(@Nullable Object left, @Nullable Object right) {
        return areEqual(left, right, UnaryOperator.identity());
    }

    /**
     * Compares the given <tt>Strings</tt> just like {@link String#compareTo(String)}
     * but with graceful handling for <tt>null</tt> values.
     *
     * @param left  the first string to be compared
     * @param right the second string to be compared with
     * @return <tt>true</tt> if both values are empty or if both strings are equal - <tt>false</tt> otherwise
     */
    public static boolean areTrimmedEqual(@Nullable Object left, @Nullable Object right) {
        return areEqual(left, right, Strings::trim);
    }

    /**
     * Returns a string representation of the given object while gracefully handling <tt>null</tt> values.
     * <p>
     * Internally this method calls {@link Object#toString()}. For locale aware or locale fixed methods,
     * {@link sirius.kernel.nls.NLS#toUserString(Object)} and
     * {@link sirius.kernel.nls.NLS#toMachineString(Object)} can be used.
     *
     * @param object the object to be converted to string.
     * @return the string representation of the given object or <tt>null</tt> if <tt>object</tt> was null.
     */
    @Nullable
    public static String toString(@Nullable Object object) {
        return object == null ? null : object.toString();
    }

    /**
     * Formats the given pattern string <tt>format</tt> with the given <tt>arguments</tt>.
     * <p>
     * This is just a delegate to {@link String#format(String, Object...)}. It is however defined in this class to
     * force all framework parts to use the same formatting mechanism (and not <tt>MessageFormat</tt> etc.).
     * <p>
     * This method is intended to be used for format short strings or non-translated log messages etc. For more
     * complex messages and especially for translated strings, a {@link sirius.kernel.nls.Formatter} should be
     * used.
     *
     * @param format    the format pattern to be used
     * @param arguments the parameters for be used for replacement
     * @return a formatted string as defined in <tt>String#format</tt>
     * @see String#format(String, Object...)
     * @see sirius.kernel.nls.Formatter
     * @see sirius.kernel.nls.NLS#fmtr(String)
     */
    public static String apply(String format, Object... arguments) {
        return arguments.length == 0 ? format : String.format(format, arguments);
    }

    /**
     * Returns the first non-empty value of the given array.
     * <p>
     * This can be used to provide a default value or to check several sources for a value, e.g.:
     * <pre>
     * {@code
     *         String s = Strings.firstFilled(System.getProperty("foo.test"),
     *                                        System.getProperty("test"),
     *                                        "default");
     * }
     * </pre>
     *
     * @param values an array of string values to be scanned
     * @return the first value of values which is filled.
     * Returns <tt>null</tt> if all are empty or if no values where passed in
     */
    @Nullable
    public static String firstFilled(String... values) {
        if (values != null) {
            for (String s : values) {
                if (isFilled(s)) {
                    return s;
                }
            }
        }
        return null;
    }

    /**
     * Returns if the given string is an HTTP(S) URL.
     *
     * @param value the string to check
     * @return <tt>true</tt> if the given string is an HTTP(S) URL, <tt>false</tt> otherwise
     */
    public static boolean isHttpUrl(@Nullable String value) {
        return isUrl(value,
                     url -> "http".equalsIgnoreCase(url.getProtocol()) || "https".equalsIgnoreCase(url.getProtocol()));
    }

    /**
     * Returns if the given string is an HTTPS URL, explicitly excluding unencrypted HTTP URLs.
     *
     * @param value the string to check
     * @return <tt>true</tt> if the given string is an HTTPS URL, <tt>false</tt> otherwise
     */
    public static boolean isHttpsUrl(@Nullable String value) {
        return isUrl(value, url -> "https".equalsIgnoreCase(url.getProtocol()));
    }

    protected static boolean isUrl(@Nullable String value, Predicate<URL> checker) {
        if (isEmpty(value)) {
            return false;
        }

        try {
            return checker.test(URI.create(value).toURL());
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * Returns an url encoded representation of the given <tt>value</tt> with <tt>UTF-8</tt> as character encoding.
     *
     * @param value the value to be encoded.
     * @return an url encoded representation of value, using UTF-8 as character encoding.
     */
    @Nullable
    public static String urlEncode(@Nullable String value) {
        if (isFilled(value)) {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        }
        return value;
    }

    /**
     * Returns an url decoded representation of the given <tt>value</tt> with <tt>UTF-8</tt> as character encoding.
     *
     * @param value the value to be decoded.
     * @return an url decoded representation of value, using UTF-8 as character encoding.
     */
    @Nullable
    public static String urlDecode(@Nullable String value) {
        if (isFilled(value)) {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        }
        return value;
    }

    /**
     * Splits the given string at the first occurrence of the separator.
     * <p>
     * If the given input is empty, a tuple with <tt>null</tt> as first and second component will be returned.
     *
     * @param input     the input to be split
     * @param separator the separator used to split at
     * @return a <tt>Tuple</tt> containing the part before the separator as first
     * and the part after the separator as second component
     */
    public static Tuple<String, String> split(String input, String separator) {
        Tuple<String, String> result = Tuple.create();
        if (isFilled(input)) {
            int idx = input.indexOf(separator);
            if (idx > -1) {
                result.setFirst(input.substring(0, idx));
                result.setSecond(input.substring(idx + separator.length()));
            } else {
                result.setFirst(input);
            }
        }
        return result;
    }

    /**
     * Split a string into multiple lines with a width of at most maxCharacters.
     *
     * @param input         the string to split
     * @param maxCharacters the maximum amount of characters per line
     * @return the resulting lines
     */
    public static List<String> splitSmart(String input, int maxCharacters) {
        List<String> result = new ArrayList<>();
        if (Strings.isEmpty(input)) {
            return result;
        }
        StringBuilder current = new StringBuilder();
        for (String toAdd : splitIntoWords(input, maxCharacters)) {
            if (!current.isEmpty() && current.length() + toAdd.length() >= maxCharacters) {
                result.add(current.toString());
                current = new StringBuilder();
            }
            if (!current.isEmpty()) {
                current.append(" ");
            }
            current.append(toAdd);
        }
        if (!current.isEmpty()) {
            result.add(current.toString());
        }
        return result;
    }

    /**
     * Split a string into words of at most maxCharacters length.
     * <p>
     * If a word is longer than maxCharacters, it is split into multiple parts.
     *
     * @param input         the input string
     * @param maxCharacters the maximum number of characters a word may have
     * @return a list of the words of the input string
     */
    @SuppressWarnings("java:S6204")
    @Explain("We provide a mutable list here, as we cannot predict the callers intentions.")
    private static List<String> splitIntoWords(String input, int maxCharacters) {
        return Arrays.stream(input.split(" ")).<String>mapMulti((string, consumer) -> {
            while (string.length() > maxCharacters) {
                consumer.accept(string.substring(0, maxCharacters));
                string = string.substring(maxCharacters);
            }
            consumer.accept(string);
        }).filter(Strings::isFilled).collect(Collectors.toList());
    }

    /**
     * Splits the given string at the last occurrence of the separator.
     * <p>
     * If the given input is empty, a tuple with <tt>null</tt> as first and second component will be returned.
     *
     * @param input     the input to be split
     * @param separator the separator used to split at
     * @return a <tt>Tuple</tt> containing the part before the separator as first
     * and the part after the separator as second component
     */
    public static Tuple<String, String> splitAtLast(String input, String separator) {
        Tuple<String, String> result = Tuple.create();
        if (isFilled(input)) {
            int idx = input.lastIndexOf(separator);
            if (idx > -1) {
                result.setFirst(input.substring(0, idx));
                result.setSecond(input.substring(idx + separator.length()));
            } else {
                result.setFirst(input);
            }
        }
        return result;
    }

    /**
     * Limits the length of the given string to the given length.
     *
     * @param input  the object which string representation should be limited to the given length
     * @param length the max. number of characters to return
     * @return a part of the string representation of the given <tt>input</tt>. If input is shorter
     * than <tt>length</tt>, the full value is returned. If input is <tt>null</tt>, "" is returned.
     */
    public static String limit(@Nullable Object input, int length) {
        return limit(input, length, false);
    }

    /**
     * Limits the length of the given string to the given length.
     *
     * @param input        the object which string representation should be limited to the given length
     * @param length       the max. number of characters to return. Note: If the parameter is less than 1 an empty string is returned.
     * @param showEllipsis whether to append three dots if <tt>input</tt> is longer than <tt>length</tt>
     * @return a part of the string representation of the given <tt>input</tt>. If input is shorter
     * than <tt>length</tt>, the trimmed input value is returned. If input is <tt>null</tt> or empty, "" is returned. This also applies if <tt>length</tt> is less than 1.
     */
    public static String limit(@Nullable Object input, int length, boolean showEllipsis) {
        if (isEmpty(input) || length <= 0) {
            return "";
        }
        String trimmedInputString = String.valueOf(input).trim();
        if (trimmedInputString.length() > length) {
            return trimmedInputString.substring(0, (showEllipsis ? length - ELLIPSIS.length() : length))
                   + (showEllipsis ? ELLIPSIS : "");
        } else {
            return trimmedInputString;
        }
    }

    /**
     * Truncates the given input in the middle by preserving characters from the start and end.
     * <p>
     * Note:
     * Adds a truncated signal in the form of "…[…]…" in the middle of the string. This signal consist of 5 chars.
     * The chars of the signal are considered when determining if a truncation is necessary. Therefore, a truncation only
     * takes place if the input string is longer than <tt>charsToPreserveFromStart</tt> + <tt>charsToPreserveFromEnd</tt> + length of the truncated signal.
     *
     * @param input                    the input to truncate
     * @param charsToPreserveFromStart the number of characters to preserve from the start. Note that this value must be greater than or equal to 0.
     * @param charsToPreserveFromEnd   the number of characters to preserve from the end. Note that this value must be greater than or equal to 0.
     * @return a part of the string representation of the given <tt>input</tt> with a truncated signal added in the middle.
     * If input is shorter than or equal to (<tt>charsToPreserveFromStart</tt> + <tt>charsToPreserveFromEnd</tt> + length of the truncated signal), the trimmed input value is returned.
     * If input is <tt>null</tt> or empty, "" is returned. This also applies if (<tt>charsToPreserveFromStart</tt> + <tt>charsToPreserveFromEnd</tt>) is 0.
     */
    public static String truncateMiddle(@Nullable Object input,
                                        int charsToPreserveFromStart,
                                        int charsToPreserveFromEnd) {
        int charsToPreserve = charsToPreserveFromStart + charsToPreserveFromEnd;
        if (isEmpty(input) || charsToPreserveFromStart < 0 || charsToPreserveFromEnd < 0 || charsToPreserve == 0) {
            return "";
        }

        String trimmedInputString = String.valueOf(input).trim();
        if (trimmedInputString.length() <= charsToPreserve + TRUNCATED_SIGNAL.length()) {
            return trimmedInputString;
        }

        String start = trimmedInputString.substring(0, charsToPreserveFromStart).trim();
        String end = trimmedInputString.substring(trimmedInputString.length() - charsToPreserveFromEnd).trim();
        return start + TRUNCATED_SIGNAL + end;
    }

    /**
     * Returns a string representation of the given map.
     * <p>
     * Keys and values are separated by a colon (:) and entries by a new line.
     *
     * @param source to map to be converted to a string
     * @return a string representation of the given map, or "" if the map was null
     */
    @Nonnull
    public static String join(@Nullable Map<?, ?> source) {
        if (source == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            sb.append(entry.getKey());
            sb.append(": ");
            sb.append(entry.getValue());
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Returns a string concatenation of the given lists items.
     * <p>
     * Generates a string which contains the string representation of each item separated by the given separator.
     * The conversion method for the list items used is {@link NLS#toMachineString(Object)}. This method will filter
     * empty values (<tt>""</tt> or <tt>null</tt>) and ignore those.
     *
     * @param list      the list items to join
     * @param separator the separator to place between the items
     * @return a string of all items joined together and separated by the given separator. Returns "" is the list was
     * <tt>null</tt> or empty.
     */
    @Nonnull
    public static String join(@Nullable Iterable<?> list, @Nonnull String separator) {
        if (list == null) {
            return "";
        }

        StringBuilder result = null;
        for (Object item : list) {
            if (Strings.isEmpty(item)) {
                continue;
            }
            if (result != null) {
                result.append(separator);
            } else {
                result = new StringBuilder();
            }
            result.append(NLS.toMachineString(item));
        }

        return result != null ? result.toString() : "";
    }

    /**
     * Returns a string concatenation of the given array items.
     * <p>
     * Generates a string which contains the string representation of each item separated by the given separator.
     * This method will filter empty values (<tt>""</tt> or <tt>null</tt>) and ignore those.
     *
     * @param separator the separator to place between the items
     * @param parts     the array of items to join
     * @return a string of all items joined together and separated by the given separator. Returns "" is the array was
     * empty.
     */
    @Nonnull
    public static String join(@Nonnull String separator, @Nonnull String... parts) {
        return join(Arrays.asList(parts), separator);
    }

    /**
     * Generates a random password with 7 characters length.
     *
     * @return a randomly generated password.
     */
    public static String generatePassword() {
        return generateCode(7);
    }

    /**
     * Generates a string of the given length, containing random character.
     *
     * @param length the desired length of the generated string.
     * @return a string with the given length, consisting of random characters.
     */
    public static String generateCode(int length) {
        StringBuilder sb = new StringBuilder();
        SecureRandom rnd = new SecureRandom();
        for (int i = 0; i < length; i++) {
            sb.append(VALID_CODE_CHARS[rnd.nextInt(VALID_CODE_CHARS.length)]);
        }
        return sb.toString();
    }

    /**
     * Replaces german umlauts to HTML entities as some email clients fail otherwise. Using UTF-8 as encoding this
     * shouldn't normally be necessary and is just there to support legacy software.
     *
     * @param input the input to escape
     * @return a string where all known (supported) umlauts are replaced by HTML entities
     */
    public static String replaceUmlautsToHtml(String input) {
        String textToReplace = input;
        textToReplace = textToReplace.replace("ö", "&ouml;");
        textToReplace = textToReplace.replace("ä", "&auml;");
        textToReplace = textToReplace.replace("ü", "&uuml;");
        textToReplace = textToReplace.replace("ß", "&szlig;");
        textToReplace = textToReplace.replace("Ö", "&Ouml;");
        textToReplace = textToReplace.replace("Ä", "&Auml;");
        textToReplace = textToReplace.replace("Ü", "&Uuml;");
        return textToReplace;
    }

    /**
     * Returns a trimmed version of the given object's string representation.
     * And empty string '' will always be null.
     *
     * @param object the input to be converted into a string and then trimmed
     * @return a trimmed version of the string representation of the given object.
     * Returns <tt>null</tt> if an empty string was given.
     */
    @Nullable
    public static String trim(Object object) {
        if (isEmpty(object)) {
            return null;
        }
        return object.toString().trim();
    }

    /**
     * Applies the given list of cleanups on the given string.
     * <p>
     * Note that empty/<tt>null</tt> inputs will always result in an empty string.
     *
     * @param inputString the string to clean-up
     * @param cleanups    the operations to perform, most probably some from {@link StringCleanup}
     * @return the cleaned up string
     * @see StringCleanup
     */
    @Nonnull
    @SafeVarargs
    public static String cleanup(@Nullable String inputString, @Nonnull UnaryOperator<String>... cleanups) {
        if (Strings.isEmpty(inputString)) {
            return "";
        }

        String value = inputString;
        for (UnaryOperator<String> cleanup : cleanups) {
            value = cleanup.apply(value);
        }

        return value;
    }

    /**
     * Applies the given list of cleanups on the given string.
     * <p>
     * Note that empty/<tt>null</tt> inputs will always result in an empty string.
     *
     * @param inputString the string to clean-up
     * @param cleanups    the operations to perform, most probably some from {@link StringCleanup}
     * @return the cleaned up string
     * @see StringCleanup
     */
    @Nonnull
    @SuppressWarnings("java:S2637")
    @Explain("isEmpty properly handles null cases")
    public static String cleanup(@Nullable String inputString, @Nonnull Iterable<UnaryOperator<String>> cleanups) {
        if (Strings.isEmpty(inputString)) {
            return "";
        }

        String value = inputString;
        for (UnaryOperator<String> cleanup : cleanups) {
            value = cleanup.apply(value);
        }

        return value;
    }

    protected static final String REGEX_DETECT_XML_TAGS = "</?[a-zA-Z][^>]*>";
    protected static final String REGEX_DETECT_ENTITIES = "&#?[a-zA-Z0-9]+;";
    /**
     * Defines a pattern (regular expression) to detect XML tags and entities.
     */
    public static final Pattern PATTERN_DETECT_XML =
            Pattern.compile(Strings.join("|", REGEX_DETECT_XML_TAGS, REGEX_DETECT_ENTITIES));

    /**
     * Determines if the given content contains XML tags or entities.
     *
     * @param content the content to check
     * @return <tt>true</tt> if XML tags or entities were found, <tt>false</tt> otherwise
     */
    public static boolean probablyContainsXml(@Nullable String content) {
        if (Strings.isEmpty(content)) {
            return false;
        }

        return PATTERN_DETECT_XML.matcher(content).find();
    }

    protected static final Pattern DETECT_ALLOWED_HTML_REGEX =
            Pattern.compile("</?(" + String.join("|", StringCleanup.ALLOWED_HTML_TAG_NAMES) + ")\\b[^>]*>",
                            Pattern.CASE_INSENSITIVE);

    /**
     * Determines if the given content contains HTML tags that are {@linkplain #DETECT_ALLOWED_HTML_REGEX allowed} in
     * the system.
     *
     * @param content the content to check
     * @return <tt>true</tt> if allowed HTML tags were found, <tt>false</tt> otherwise
     */
    public static boolean containsAllowedHtml(@Nullable String content) {
        if (Strings.isEmpty(content)) {
            return false;
        }

        return DETECT_ALLOWED_HTML_REGEX.matcher(content).find();
    }

    /**
     * Removes all umlauts and other decorated latin characters.
     *
     * @param input the term to reduce characters in
     * @return the term with all decorated latin characters replaced
     * @deprecated Use {@link StringCleanup#reduceCharacters(String)} or
     * * {@code Strings.cleanup(input, Cleanup::reduceCharacters)} instead
     */
    @Deprecated
    public static String reduceCharacters(String input) {
        return StringCleanup.reduceCharacters(input);
    }

    /**
     * Shortens a string to the given number of chars,
     * cutting of at most half of the string and adding ... if something has been cut of.
     *
     * @param string   string to be cut of
     * @param numChars new maximum length of string
     * @return the shortened string
     * @see Strings#limit(Object, int)
     * @see Strings#limit(Object, int, boolean)
     */
    public static String shorten(String string, int numChars) {
        if (isEmpty(string)) {
            return "";
        }
        if (string.length() <= numChars) {
            return string;
        }
        int index = numChars - 1;
        int maxCutoff = Math.min(string.length() / 2, 10);
        while (numChars > 0 && Character.isLetterOrDigit(string.charAt(index)) && maxCutoff > 0) {
            index--;
            maxCutoff--;
        }
        return string.substring(0, index) + "...";
    }

    /**
     * Replaces all occurrences of the given regular expression by the result of the given replacement function.
     * <p>
     * The regular expression is expected to have one explicit matching group which will be used as input for
     * the replacement function.
     * <p>
     * To replace all occurrences of {@code #{X}} by {@code NLS.get("X")} one could use:
     * {@code Strings.replaceAll(Pattern.compile("#\\{([^\\}]+)\\}"), someText, NLS::get)}
     *
     * @param regEx       the regular expression to replace in the given input
     * @param input       the input to scan
     * @param replacement the replacement function which transforms the first group of the match into the string used
     *                    as replacement for the whole match.
     * @return the input string where are occurrences of the given regular expression have been replaced by the result
     * of the replacement function.
     */
    public static String replaceAll(Pattern regEx, String input, UnaryOperator<String> replacement) {
        if (isEmpty(input)) {
            return input;
        }
        Matcher m = regEx.matcher(input);
        boolean result = m.find();
        if (result) {
            StringBuilder sb = new StringBuilder();
            do {
                m.appendReplacement(sb, replacement.apply(m.group(1)));
                result = m.find();
            } while (result);
            m.appendTail(sb);
            return sb.toString();
        }
        return input;
    }

    /**
     * Pads the given string on the left side to the given length using the given padding.
     * <p>
     * Note that if <tt>padding</tt> consists of several characters, the final string might be longer than
     * <tt>minLength</tt> as no substring but only the full value of <tt>padding</tt> is used to pad.
     * <p>
     * <b>Implementation detail:</b> This method checks if padding is necessary at all. If not, it directly returns the
     * given input. This should enable inlining and therefore create a fast path if no padding is necessary.
     *
     * @param input     the input to pad
     * @param padding   the padding to use
     * @param minLength the minimal length to reach
     * @return a string which is at least <tt>minLength</tt> characters long
     */
    public static String leftPad(String input, String padding, int minLength) {
        if (input != null && input.length() > minLength) {
            return input;
        }

        return performPadding(input, padding, minLength, true);
    }

    /**
     * Pads the given string on the right side to the given length using the given padding.
     * <p>
     * Note that if <tt>padding</tt> consists of several characters, the final string might be longer than
     * <tt>minLength</tt> as no substring but only the full value of <tt>padding</tt> is used to pad.
     * <p>
     * <b>Implementation detail:</b> This method checks if padding is necessary at all. If not, it directly returns the
     * given input. This should enable inlining and therefore create a fast path if no padding is necessary.
     *
     * @param input     the input to pad
     * @param padding   the padding to use
     * @param minLength the minimal length to reach
     * @return a string which is at least <tt>minLength</tt> characters long
     */
    public static String rightPad(String input, String padding, int minLength) {
        if (input != null && input.length() > minLength) {
            return input;
        }

        return performPadding(input, padding, minLength, false);
    }

    /**
     * Pads the given string either on the left or on the right side, to the min length using the given padding.
     * <p>
     * Although this method looks a bit complex and using a boolean parameter to determine which side to pad is fishy,
     * this approach maximizes efficiency by using a single <tt>StringBuilder</tt> (if required at all).
     *
     * @param input     the input to pad
     * @param padding   the padding to use
     * @param minLength the minimal length to reach
     * @param left      determines if the padding should be placed on the left or on the right size
     * @return a string which is at least <tt>minLength</tt> characters long
     */
    private static String performPadding(@Nullable String input, @Nonnull String padding, int minLength, boolean left) {
        int numberOfPaddings = minLength;
        if (input != null) {
            numberOfPaddings -= input.length();
        }

        if (padding.length() > 1) {
            numberOfPaddings = (int) Math.ceil((double) numberOfPaddings / padding.length());
        }

        if (numberOfPaddings <= 0) {
            return input;
        }

        StringBuilder sb = new StringBuilder();
        if (!left && input != null) {
            sb.append(input);
        }

        sb.append(padding.repeat(numberOfPaddings));

        if (left && input != null) {
            sb.append(input);
        }

        return sb.toString();
    }
}
