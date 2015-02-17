/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.nls;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import sirius.kernel.commons.Strings;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * An alternative for <tt>MessageFormat</tt> which generates strings by replacing named parameters in a given template.
 * <p>
 * A formatter is created for a given template string which contains named parameters like <code>${param1}</code>.
 * Using one of the <tt>set</tt> methods, values for the parameters can be supplied. Calling <code>#format</code>
 * creates the output string.
 * <p>
 * Non string objects which are passed in as parameters, will be converted using {@link NLS#toUserString(Object)}
 * <p>
 * A formatter is neither thread safe nor intended for reuse. Instead a formatter is created, supplied with the
 * relevant parameters by chaining calls to <tt>set</tt> and then discarded after getting the result string via
 * <tt>format</tt>.
 * <p>
 * An example call might look like this:
 * <pre>
 * <code>
 *         System.out.println(
 *              Formatter.create("Hello ${programmer}")
 *                       .set("programmer, "Obi Wan")
 *                       .format());
 * </code>
 * </pre>
 * <p>
 * {@link NLS} uses this class by supplied translated patterns when calling {@link NLS#fmtr(String)}.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @see NLS#fmtr(String)
 * @since 2013/08
 */
public class Formatter {
    private boolean urlEncode = false;
    private boolean smartFormat = false;
    private Map<String, String> replacement = Maps.newTreeMap();
    private String pattern;
    private String lang;
    public static final Pattern PARAM = Pattern.compile("\\$\\{([A-Za-z0-9\\.]+)\\}");

    /**
     * Use the static factory methods <tt>create</tt> to obtain a new instance.
     */
    protected Formatter() {
        super();
    }

    /**
     * Creates a new formatter with the given pattern and language.
     * <p>
     * The given language will be used when converting non-string parameters.
     *
     * @param pattern specifies the pattern to be used for creating the output
     * @param lang    specifies the language used when converting non-string parameters.
     * @return <tt>this</tt> for fluently calling <tt>set</tt> methods.
     */
    public static Formatter create(String pattern, String lang) {
        Formatter result = new Formatter();
        result.pattern = pattern;
        result.lang = lang;
        return result;
    }

    /**
     * Creates a new formatter with the given pattern.
     * <p>
     * Uses the currently active language when converting non-string parameters.
     *
     * @param pattern specifies the pattern to be used for creating the output
     * @return <tt>this</tt> for fluently calling <tt>set</tt> methods.
     */
    public static Formatter create(String pattern) {
        Formatter result = new Formatter();
        result.pattern = pattern;
        result.lang = NLS.getCurrentLang();
        return result;
    }


    /**
     * Creates a new formatter with auto url encoding turned on.
     * <p>
     * Any parameters passed to this formatter will be automatically url encoded.
     *
     * @param pattern specifies the pattern to be used for creating the output
     * @return <tt>this</tt> for fluently calling <tt>set</tt> methods.
     */
    public static Formatter createURLFormatter(String pattern) {
        Formatter result = new Formatter();
        result.pattern = pattern;
        result.urlEncode = true;
        return result;
    }

    /**
     * Adds the replacement value to use for the given <tt>property</tt>.
     *
     * @param property the parameter in the template string which should be replaced
     * @param value    the value which should be used as replacement
     * @return <tt>this</tt> to permit fluent method chains
     */
    public Formatter set(String property, Object value) {
        setDirect(property, NLS.toUserString(value, lang), urlEncode);
        return this;
    }

    /**
     * Adds the replacement value to use for the given <tt>property</tt>, without url encoding the value.
     * <p>
     * Formatters created by <tt>#createURLFormatter</tt> perform automatic url conversion for all parameters.
     * Using this method however, disables url encoding for the given parameter and value.
     *
     * @param property the parameter in the template string which should be replaced
     * @param value    the value which should be used as replacement
     * @return <tt>this</tt> to permit fluent method chains
     */
    public Formatter setUnencoded(String property, Object value) {
        return setDirect(property, NLS.toUserString(value, lang), false);
    }

    /**
     * Sets the whole context as parameters in this formatter.
     * <p>
     * Calls <tt>#set</tt> for each entry in the given map.
     *
     * @param ctx a <tt>Map</tt> which provides a set of entries to replace.
     * @return <tt>this</tt> to permit fluent method chains
     */
    public Formatter set(Map<String, Object> ctx) {
        if (ctx != null) {
            for (Map.Entry<String, Object> e : ctx.entrySet()) {
                set(e.getKey(), e.getValue());
            }
        }
        return this;
    }

    /**
     * Directly sets the given string value for the given property.
     * <p>
     * Sets the given string as replacement value for the named parameter. The value will not be sent through
     * {@link NLS#toUserString(Object)} and therefore not trimmed etc.
     *
     * @param property  the parameter in the template string which should be replaced
     * @param value     the value which should be used as replacement
     * @param urlEncode determines if url encoding should be applied. If the parameter is set to <tt>false</tt>,
     *                  this method won't perform any url encoding, even if the formatter was created
     *                  using <tt>#createURLFormatter</tt>
     * @return <tt>this</tt> to permit fluent method chains
     */
    public Formatter setDirect(String property, String value, boolean urlEncode) {
        replacement.put(property, urlEncode ? Strings.urlEncode(value) : value);
        return this;
    }

    /**
     * Generates the formatted string.
     * <p>
     * Applies all supplied replacement values on detected parameters formatted like <code>${param}</code>.
     *
     * @return the template string with all parameters replaced for which a value was supplied.
     * @throws java.lang.IllegalArgumentException if the pattern is malformed
     */
    public String format() {
        return format(false);
    }

    /**
     * Generates the formatted string using smart output formatting.
     * <p>
     * Applies all supplied replacement values on detected parameters formatted like <code>${param}</code>.
     * Block can be formed using '[' and ']' a whole block is only output, if at least one replacement
     * was not empty.
     * <p>
     * Consider the pattern <code>[${salutation} ][${firstname}] ${lastname}</code>. This will create
     * <tt>Mr. Foo Bar</tt> if all three parameters are filled, but <tt>Mr. Bar</tt> if the first name is missing
     * or <tt>Foo Bar</tt> if the salutation is missing.
     *
     * @return the template string with all parameters replaced for which a value was supplied.
     * @throws java.lang.IllegalArgumentException if the pattern is malformed
     */
    public String smartFormat() {
        return format(true);
    }

    /*
     * Keeps track of the current smart formatting block being parsed.
     */
    private static class Block {
        StringBuilder output = new StringBuilder();
        boolean replacementFound = false;
        int startIndex;
    }

    /*
     * Stack based implementation parsing parameterized strings with smart blocks. Each nested block will
     * result in one stack level.
     */
    private String format(boolean smart) {
        List<Block> blocks = Lists.newArrayList();
        Block currentBlock = new Block();
        blocks.add(currentBlock);
        int index = 0;
        while (index < pattern.length()) {
            char current = pattern.charAt(index);
            if (current == '$' && pattern.charAt(index + 1) == '{') {
                index = performParameterReplacement(currentBlock, index);
            } else if (current == '[' && smart) {
                currentBlock = startBlock(blocks, index);
            } else if (current == ']' && smart) {
                currentBlock = endBlock(blocks, currentBlock, index);
            } else {
                currentBlock.output.append(current);
            }
            index++;
        }
        if (blocks.size() > 1) {
            throw new IllegalArgumentException(Strings.apply(
                    "Unexpected end of pattern. Expected ']' for '[' at index %d in '%s'",
                    currentBlock.startIndex + 1,
                    pattern));
        } else {
            return currentBlock.output.toString();
        }
    }

    private int performParameterReplacement(Block currentBlock, int index) {
        index += 2;
        int keyStart = index;
        while (index < pattern.length() && pattern.charAt(index) != '}') {
            index++;
        }
        if (index >= pattern.length()) {
            throw new IllegalArgumentException(Strings.apply("Missing } for ${ started at index %d in '%s'",
                                                             keyStart - 1,
                                                             pattern));
        }
        String key = pattern.substring(keyStart, index);
        String value = replacement.computeIfAbsent(key, s -> {
            throw new IllegalArgumentException(Strings.apply("Unknown value '%s' used at index %d in '%s'",
                                                             key,
                                                             keyStart - 1,
                                                             pattern));
        });
        if (Strings.isFilled(value)) {
            currentBlock.output.append(value);
            currentBlock.replacementFound = true;
        }
        return index;
    }

    private Block startBlock(List<Block> blocks, int index) {
        Block currentBlock;
        currentBlock = new Block();
        currentBlock.startIndex = index;
        blocks.add(currentBlock);
        return currentBlock;
    }

    private Block endBlock(List<Block> blocks, Block currentBlock, int index) {
        if (blocks.size() == 1) {
            throw new IllegalArgumentException(Strings.apply("Unexpected ']' at index %d in '%s'", index + 1, pattern));
        } else {
            if (currentBlock.replacementFound) {
                Block next = blocks.get(blocks.size() - 2);
                next.output.append(currentBlock.output);
                next.replacementFound = true;
            }
            currentBlock = blocks.get(blocks.size() - 2);
            blocks.remove(blocks.size() - 1);
        }
        return currentBlock;
    }

    @Override
    public String toString() {
        return format();
    }
}
