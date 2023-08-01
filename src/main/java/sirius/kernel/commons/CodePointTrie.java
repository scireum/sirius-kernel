/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Implements the {@link BaseTrie} to use keys based on {@link Integer} code points.
 *
 * @param <V> the type of values managed by the trie
 */
public class CodePointTrie<V> extends BaseTrie<V> {

    /**
     * Creates a new {@link CodePointTrie} without forcing you to re-type the generics.
     *
     * @param <V> the type of values managed by the trie
     * @return a new instance of {@link CodePointTrie}
     */
    public static <V> CodePointTrie<V> create() {
        return new CodePointTrie<>();
    }

    @Override
    protected IntStream stream(CharSequence string) {
        return string.codePoints();
    }

    @Override
    protected String assembleString(List<Integer> keys) {
        StringBuilder builder = new StringBuilder();
        keys.forEach(builder::appendCodePoint);
        return builder.toString();
    }
}
