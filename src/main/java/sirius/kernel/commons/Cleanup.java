/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import java.util.Set;

/**
 * Defines a set of cleanup operations that can be applied to a string.
 *
 * @see Strings#cleanup(String, Set)
 */
public enum Cleanup {

    /**
     * Removes any ASCII control characters from the string.
     */
    REMOVE_CONTROL_CHARS,

    /**
     * Reduces all umlauts and decorated latin characters to their base character.
     *
     * @see Strings#reduceCharacters(String)
     */
    REDUCE_CHARACTERS,

    /**
     * Reduces multiple whitespaces to a single <tt>blank</tt>.
     */
    REDUCE_WHITESPACES,

    /**
     * Removes all whitespaces from the string.
     */
    REMOVE_WHITESPACES,

    /**
     * Trims whitespace from the string.
     */
    TRIM,

    /**
     * Transforms the string into lower case.
     */
    LOWERCASE,

    /**
     * Transforms the string into upper case.
     */
    UPPERCASE,

    /**
     * Removes all punctuation from the string.
     */
    REMOVE_PUNCTUATION,
}
