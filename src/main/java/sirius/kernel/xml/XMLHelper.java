/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import sirius.kernel.commons.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.StringCharacterIterator;

/**
 * Provides utility functions for processing XML.
 */
public class XMLHelper {

    private XMLHelper() {
    }

    /**
     * Escapes all XML control characters: <tt>&lt;, &gt;, &quot;, &#039; and &amp;</tt>
     * <p>
     *     These will be replaced with the proper XML entities, e.g. <tt>&gt;</tt> becomes <tt>&amp;gt;</tt>
     *
     * @param value the string value to escape
     * @return the string with their values escaped
     */
    @Nonnull
    public static String escapeXML(@Nullable String value) {
        if (Strings.isEmpty(value)) {
            return "";
        } else {
            StringBuilder result = new StringBuilder();
            StringCharacterIterator iterator = new StringCharacterIterator(value);

            for (char character = iterator.current(); character != '\uffff'; character = iterator.next()) {
                if (character == 60) {
                    result.append("&lt;");
                } else if (character == 62) {
                    result.append("&gt;");
                } else if (character == 34) {
                    result.append("&quot;");
                } else if (character == 39) {
                    result.append("&#039;");
                } else if (character == 38) {
                    result.append("&amp;");
                } else {
                    result.append(character);
                }
            }

            return result.toString();
        }
    }
}
