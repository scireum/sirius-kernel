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
 * Implements the {@link BaseTrie} to use {@link Character}-based keys.
 *
 * @param <V> the type of values managed by the trie
 */
public class Trie<V> extends BaseTrie<V> {

    /**
     * Creates a new {@link Trie} without forcing you to re-type the generics.
     *
     * @param <V> the type of values managed by the trie
     * @return a new instance of {@link Trie}
     */
    public static <V> Trie<V> create() {
        return new Trie<>();
    }

    @Override
    protected IntStream stream(CharSequence string) {
        return string.chars();
    }

    @Override
    protected String assembleString(List<Integer> keys) {
        StringBuilder builder = new StringBuilder();
        keys.stream().map(integer -> (char) integer.intValue()).forEach(builder::append);
        return builder.toString();
    }
}
