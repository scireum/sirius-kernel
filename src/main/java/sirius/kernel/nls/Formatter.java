/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.nls;

import com.google.common.collect.Maps;
import sirius.kernel.commons.Strings;

import java.util.Map;
import java.util.regex.Matcher;
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
        return setDirect(property, NLS.toUserString(value == null ? "" : value, lang), false);
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
     * If no value is supplied for a parameter, the original expression will remain in the string.
     *
     * @return the template string with all parameters replaced for which a value was supplied.
     */
    public String format() {
        StringBuilder sb = new StringBuilder();
        int index = 0;
        Matcher pm = PARAM.matcher(pattern);
        while (pm.find(index)) {
            sb.append(pattern.substring(index, pm.start()));
            String val = replacement.get(pm.group(1));
            if (val == null) {
                sb.append(pm.group());
            } else {
                sb.append(val);
            }
            index = pm.end();
        }
        sb.append(pattern.substring(index));
        return sb.toString();
    }

    @Override
    public String toString() {
        return format();
    }
}
