/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A map like data structure which associates strings (char sequences) to values.
 * <p>
 * A trie is a highly efficient data structure for iterating through a string and retrieving a previously stored
 * value. Checking containment or retrieving a value has guaranteed O(n) runtime, where n is the length of the
 * processed string, independent of the size of the trie.
 * <p>
 * An Example: If we have a list of stop words: "one", "two", "three" and want to detect if these occur in a
 * given text, we can do the following:
 * <pre>
 * {@code
 * Trie<Boolean> trie = Trie.create();
 *
 * trie.put("one", true);
 * trie.put("two", true);
 * trie.put("three", true);
 *
 * String check = "I'd like to have three beer please";
 * Trie.ContainmentIterator<Boolean> iter = trie.iterator();
 *
 * for (int i = 0; i < check.length(); i++) {
 *     if (!iter.doContinue(check.charAt(i))) {
 *         if (iter.isCompleted()) {
 *             System.out.println("Found!");
 *         }
 *         iter.resetWith(check.charAt(i));
 *     }
 * }
 * if (iter.isCompleted()) {
 *     System.out.println("Found!");
 * }
 *
 * }
 * </pre>
 *
 * @param <V> the type of values managed by the trie
 */
public abstract class BaseTrie<V> {

    /**
     * Contains the root of the trie.
     */
    protected final Node root = new Node();

    /**
     * Represents an iterator which navigates through the trie character by character.
     *
     * @param <V> the type of values managed by the trie
     */
    public interface ContainmentIterator<V> {

        /**
         * Determines if the current path can be continued with the given character.
         * <p>
         * This will not change the internal state.
         *
         * @param c the character to continue with
         * @return <tt>true</tt> if the current path can be continued using the given character,
         * <tt>false</tt> otherwise.
         */
        boolean canContinue(int c);

        /**
         * Tries to continue the current path with the given character.
         * <p>
         * If the current path can be continued, the internal state will be updated. Otherwise, the internal
         * state will remain unchanged - the iterator is not reset automatically.
         *
         * @param c the character to continue with
         * @return <tt>true</tt> if it was possible to continue using the given character, <tt>false</tt> otherwise
         */
        boolean doContinue(int c);

        /**
         * Returns the value associated with the key represented by the path traversed so far.
         *
         * @return the value represented by the path traversed to far or <tt>null</tt> if no value is available
         */
        V getValue();

        /**
         * Sets the value to be associated with the key represented by the path traversed so far.
         *
         * @param value the value to set
         */
        void setValue(V value);

        /**
         * Determines if the iterator is currently pointing at a valid match.
         *
         * @return <tt>true</tt> if a value was previously associated with path traversed so far,
         * <tt>false</tt> otherwise
         */
        boolean isCompleted();

        /**
         * Determines if the iterator can backtrack.
         *
         * @return <tt>true</tt> if at least one transition took place, fl<tt>false</tt> if the iterator is at the
         * root node.
         */
        boolean canGoBack();

        /**
         * Undoes the latest transition to support backtracking.
         */
        void goBack();

        /**
         * Returns a set of all possible continuations for the current state of the iterator.
         *
         * @return a set of all possible characters to continue the current path
         */
        Set<Integer> getPossibilities();

        /**
         * Restarts the iterator at the beginning of the trie.
         */
        void reset();

        /**
         * Restarts the iterator at the beginning and tries to perform the next transition using the given character.
         *
         * @param c the character to try to use after resetting the iterator
         * @return <tt>true</tt> if the transition using <tt>c</tt> was possible, <tt>false</tt> otherwise. In this
         * case the iterator remains in the "reset" state and can be used as if <tt>reset()</tt> was called.
         */
        boolean resetWith(int c);
    }

    /**
     * Internal class representing a single node in the trie.
     */
    protected class Node {

        /**
         * Points to the parent node of this node.
         */
        protected Node parent;

        /**
         * Contains a sorted list of keys.
         */
        protected final List<Integer> keys = new ArrayList<>();

        /**
         * Contains the list of continuations matching the keys list.
         */
        protected final List<Node> continuations = new ArrayList<>();

        /**
         * Contains the value associated with the path to this node.
         */
        protected V value;
    }

    /**
     * Internal implementation of the ContainmentIterator.
     */
    protected class ContainmentIteratorImpl implements ContainmentIterator<V> {

        protected Node current = root;

        @Override
        public boolean canContinue(int c) {
            int index = Collections.binarySearch(current.keys, c);
            return !(index < 0 || current.keys.get(index) != c);
        }

        @Override
        public boolean doContinue(int c) {
            int index = Collections.binarySearch(current.keys, c);
            if (index < 0 || current.keys.get(index) != c) {
                return false;
            }
            current = current.continuations.get(index);
            return true;
        }

        /**
         * Adds a new step for the given character. Internally, a binary search is performed as the key-list
         * is sorted ascending.
         */
        void addStep(int c) {
            int index = Collections.binarySearch(current.keys, c);
            if (index < 0) {
                index = (index + 1) * -1;
                current.keys.add(index, c);
                Node newNode = new Node();
                newNode.parent = current;
                current.continuations.add(index, newNode);
                current = newNode;
            } else {
                current = current.continuations.get(index);
            }
        }

        @Override
        public V getValue() {
            return current.value;
        }

        @Override
        public void setValue(V value) {
            current.value = value;
        }

        @Override
        public boolean isCompleted() {
            return current.value != null;
        }

        @Override
        public boolean canGoBack() {
            return current != root;
        }

        @Override
        public void goBack() {
            current = current.parent;
        }

        @Override
        public Set<Integer> getPossibilities() {
            return new TreeSet<>(current.keys);
        }

        @Override
        public void reset() {
            current = root;
        }

        @Override
        public boolean resetWith(int c) {
            current = root;
            return doContinue(c);
        }
    }

    /**
     * Determines if the given key is contained in the trie.
     *
     * @param key the key to check for.
     * @return <tt>true</tt> if a value is associated with the path represented by the given key,
     * <tt>false</tt> otherwise
     */
    public boolean containsKey(@Nonnull CharSequence key) {
        if (Strings.isEmpty(key)) {
            throw new IllegalArgumentException("key");
        }

        ContainmentIterator<V> iterator = iterator();
        int[] sequence = stream(key).toArray();
        for (int index = 0; index < sequence.length; index++) {
            if (!iterator.doContinue(sequence[index])) {
                return false;
            }
        }
        return iterator.isCompleted();
    }

    /**
     * Generates a new iterator for the underlying trie.
     *
     * @return a new iterator to navigate through the underlying trie
     */
    public ContainmentIterator<V> iterator() {
        return new ContainmentIteratorImpl();
    }

    /**
     * Returns the value associated with the given key.
     *
     * @param key the path to navigate through
     * @return the value associated with the path defined by the given key or <tt>null</tt> if no value is present
     */
    public V get(@Nonnull CharSequence key) {
        if (Strings.isEmpty(key)) {
            throw new IllegalArgumentException("key");
        }

        ContainmentIterator<V> iterator = iterator();
        int[] sequence = stream(key).toArray();
        for (int index = 0; index < sequence.length; index++) {
            if (!iterator.doContinue(sequence[index])) {
                return null;
            }
        }
        return iterator.getValue();
    }

    /**
     * Associates the given key with the given value.
     *
     * @param key   the path to store the given value
     * @param value the value to store in the trie.
     */
    public final void put(@Nonnull CharSequence key, V value) {
        if (Strings.isEmpty(key)) {
            throw new IllegalArgumentException("key");
        }
        if (value == null) {
            throw new IllegalArgumentException("value");
        }

        ContainmentIterator<V> iterator = iterator();
        int[] sequence = stream(key).toArray();
        for (int index = 0; index < sequence.length; index++) {
            if (!iterator.doContinue(sequence[index])) {
                ((ContainmentIteratorImpl) iterator).addStep(sequence[index]);
            }
        }
        iterator.setValue(value);
    }

    /**
     * Retrieves all keys that are stored in this {@link BaseTrie}.
     *
     * @return an {@link Collections#unmodifiableSet(Set) unmodifiable set} of all keys that are stored in this {@link BaseTrie}
     */
    public Set<String> keySet() {
        return getAllKeysBeginningWith("");
    }

    /**
     * Retrieves the number of keys that are stored in this {@link BaseTrie}.
     *
     * @return the size of this {@link BaseTrie}'s {@link #keySet() key set}
     */
    public int size() {
        return keySet().size();
    }

    /**
     * Performs a prefix search within this {@link BaseTrie}'s {@link #keySet() key set}
     *
     * @param prefix to search for
     * @return an {@link Collections#unmodifiableSet(Set) unmodifiable set} holding all keys that are beginning with
     * the given <tt>prefix</tt> (may include <tt>prefix</tt> itself)
     */
    public Set<String> getAllKeysBeginningWith(CharSequence prefix) {
        if (Strings.isEmpty(prefix)) {
            prefix = "";
        }

        ContainmentIterator<V> iterator = iterator();
        int[] sequence = stream(prefix).toArray();
        for (int index = 0; index < sequence.length; index++) {
            if (!iterator.doContinue(sequence[index])) {
                return Collections.emptySet();
            }
        }

        return getAllKeysBeginningWith(stream(prefix).boxed().collect(Collectors.toCollection(LinkedList::new)),
                                       iterator);
    }

    private Set<String> getAllKeysBeginningWith(LinkedList<Integer> prefix, ContainmentIterator<V> iter) {
        if (iter.getPossibilities().isEmpty()) {
            if (iter.getValue() != null) {
                return Collections.singleton(assembleString(prefix));
            } else {
                return Collections.emptySet();
            }
        }

        Set<String> result = new HashSet<>();
        if (iter.getValue() != null) {
            result.add(assembleString(prefix));
        }
        for (Integer possibility : iter.getPossibilities()) {
            iter.doContinue(possibility);
            prefix.addLast(possibility);
            result.addAll(getAllKeysBeginningWith(prefix, iter));
            prefix.removeLast();
            iter.goBack();
        }

        return Collections.unmodifiableSet(result);
    }

    /**
     * Streams the given string into a sequence of "bits" that make up the keys of the trie. The nature of these bits
     * depends on the implementation.
     *
     * @param string the string to stream
     * @return a stream of "bits" that make up the keys of the trie
     */
    protected abstract IntStream stream(CharSequence string);

    protected abstract String assembleString(List<Integer> keys);
}
