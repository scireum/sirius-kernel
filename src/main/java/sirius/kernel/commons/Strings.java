/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import sirius.kernel.health.Exceptions;
import sirius.kernel.nls.NLS;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static Map<Integer, String> unicodeMapping = new TreeMap<>();

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
    public static boolean areEqual(@Nullable Object left, @Nullable Object right, Function<Object, Object> modifier) {
        Object effectiveLeft = modifier.apply(left);
        Object effectiveRight = modifier.apply(right);
        if (isEmpty(effectiveLeft)) {
            return isEmpty(effectiveRight);
        }
        return Objects.equal(effectiveLeft, effectiveRight);
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
        return areEqual(left, right, Function.identity());
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
     * forces all framework parts to use the same formatting mechanism (and not <tt>MessageFormat</tt> etc.).
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
     * Returns the first non empty value of the given array.
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
     * Returns an url encoded representation of the given <tt>value</tt> with <tt>UTF-8</tt> as character encoding.
     *
     * @param value the value to be encoded.
     * @return an url encoded representation of value, using UTF-8 as character encoding.
     */
    public static String urlEncode(@Nullable String value) {
        if (isFilled(value)) {
            try {
                return URLEncoder.encode(value, Charsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                // Cannot happen if Java-Version is > 1.4....
                Exceptions.ignore(e);
            }
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
     * @param length       the max. number of characters to return
     * @param showEllipsis whether to append three dots if <tt>input</tt> is longer than <tt>length</tt>
     * @return a part of the string representation of the given <tt>input</tt>. If input is shorter
     * than <tt>length</tt>, the full value is returned. If input is <tt>null</tt>, "" is returned.
     */
    public static String limit(@Nullable Object input, int length, boolean showEllipsis) {
        if (isEmpty(input)) {
            return "";
        }
        String str = String.valueOf(input).trim();
        if (str.length() > length) {
            return str.substring(0, length) + (showEllipsis ? "…" : "");
        } else {
            return str;
        }
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
     * Returns a trimmed version of the given object's string representation.
     * And empty string '' will always be null.
     *
     * @param object the input to be converted into a string and then trimmed
     * @return a trimmed version of the string representation of the given object.
     * Returns <tt>null</tt> if an empty string was given.
     */
    public static String trim(Object object) {
        if (isEmpty(object)) {
            return null;
        }
        return object.toString().trim();
    }

    /**
     * shortens a string to the given number of chars, cutting of at most half of the string and adding ... if
     * something
     * has been cut of.
     *
     * @param string   string to be cut of
     * @param numChars new maximum length of string
     * @return the shortened string
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
    public static String replaceAll(Pattern regEx, String input, Function<String, String> replacement) {
        if (isEmpty(input)) {
            return input;
        }
        Matcher m = regEx.matcher(input);
        boolean result = m.find();
        if (result) {
            StringBuffer sb = new StringBuffer();
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
     *
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
     *
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
     * Altough this method looks a bit complex and using a boolean parameter to determine which side to pad is fishy,
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

        for (int i = 0; i < numberOfPaddings; i++) {
            sb.append(padding);
        }

        if (left && input != null) {
            sb.append(input);
        }

        return sb.toString();
    }
}
