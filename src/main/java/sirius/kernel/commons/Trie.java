package sirius.kernel.commons;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
 * <code>
 * Trie&lt;Boolean&gt; trie = Trie.create();
 *
 * trie.put("one", true);
 * trie.put("two", true);
 * trie.put("three", true);
 *
 * String check = "I'd like to have three beer please";
 * Trie.ContainmentIterator&lt;Boolean&gt; iter = trie.iterator();
 *
 * for(int i = 0; i &lt; check.length(); i++) {
 *     if (!iter.doContinue(check.charAt(i))) {
 *         if (iter.isCompleted()) {
 *             System.out.println("Found!");
 *         } else {
 *             iter.resetWith(check.charAt(i));
 *         }
 *     }
 * }
 *
 * </code>
 * </pre>
 *
 * @param <V> the type of values managed by the trie
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2014/01
 */
public class Trie<V> {

    /**
     * Creates a new {@link Trie} without forcing you to re-type the generics.
     *
     * @param <V> the type of values managed by the trie
     * @return a new instance of {@link Trie}
     */
    public static <V> Trie<V> create() {
        return new Trie<>();
    }

    /**
     * Represents an iterator which navigates through the trie character by character.
     */
    public interface ContainmentIterator<V> {

        /**
         * Determines if the current path can be continued with the given character.
         * <p>
         * This will not change the internal state.
         *
         * @param c the character to continue with
         * @return <tt>true</tt> if the current path can continued using the given character,
         * <tt>false</tt> otherwise.
         */
        boolean canContinue(char c);

        /**
         * Tries to continue the current path with the given character.
         * <p>
         * If the current path can be continued, the internal state will be updated. Otherwise the internal
         * state will remain unchanged - the iterator is not reset automatically.
         *
         * @param c the character to continue with
         * @return <tt>true</tt> if it was possible to continue using the given character, <tt>false</tt> otherwise
         */
        boolean doContinue(char c);

        /**
         * Returns the value associated with the key represented by the path traversed so far.
         *
         * @return the value represented by the path traversed to far or <tt>null</tt> if no value is available
         */
        V getValue();

        /**
         * Sets the value to be associated with the key represented by the path traversed so far
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
         * Undoes the latest transition to support backtracking
         */
        void goBack();

        /**
         * Returns a set of all possible continuations for the current state of the iterator
         *
         * @return a set of all possible characters to continue the current path
         */
        Set<Character> getPossibilities();

        /**
         * Restarts the iterator at the beginning of the trie.
         */
        void reset();

        /**
         * Restarts the iterator at  the beginning and tries to perform the next transition using the given character.
         *
         * @param c the character to try to use after resetting the iterator
         * @return <tt>true</tt> if the transition using <tt>c</tt> was possible, <tt>false</tt> otherwise. In this
         * case the iterator remains in the "reset" state and can be used as if <tt>reset()</tt> was called.
         */
        boolean resetWith(char c);

    }

    /*
     * Internal class representing a single node in the trie
     */
    class Node {
        /*
         * Points to the parent node of this node
         */
        private Node parent;

        /*
         * Contains a sorted list of keys
         */
        private List<Character> keys = Lists.newArrayList();

        /*
         * Contains the list of continuations matching the keys list
         */
        private List<Node> continuations = Lists.newArrayList();

        /*
         * Contains the value associated with the path to this node
         */
        private V value;
    }

    /*
     * Contains the root of the Trie
     */
    private final Node root = new Node();

    /*
     * Internal implementation of the ContainmentIterator
     */
    private class ContainmentIteratorImpl implements ContainmentIterator<V> {

        private Node current = root;

        @Override
        public boolean canContinue(char c) {
            int index = Collections.binarySearch(current.keys, c);
            if (index < 0 || current.keys.get(index) != c) {
                return false;
            }
            return true;
        }

        public boolean doContinue(char c) {
            int index = Collections.binarySearch(current.keys, c);
            if (index < 0 || current.keys.get(index) != c) {
                return false;
            }
            current = current.continuations.get(index);
            return true;
        }

        /*
         * Adds a new step for the given character. Internally, a binary search is performed as the keylist
         * is sorted ascending.
         */
        void addStep(char c) {
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
        public Set<Character> getPossibilities() {
            return Sets.newTreeSet(current.keys);
        }

        @Override
        public void reset() {
            current = root;
        }

        @Override
        public boolean resetWith(char c) {
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
        ContainmentIterator<V> iter = iterator();
        for (int i = 0; i < key.length(); i++) {
            if (!iter.doContinue(key.charAt(i))) {
                return false;
            }
        }
        return iter.isCompleted();
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
        ContainmentIterator<V> iter = iterator();
        for (int i = 0; i < key.length(); i++) {
            if (!iter.doContinue(key.charAt(i))) {
                return null;
            }
        }
        return iter.getValue();
    }

    /**
     * Associates the given key with the given value.
     *
     * @param key   the path to store the given value
     * @param value the value to store in the trie.
     */
    public void put(@Nonnull CharSequence key, @Nonnull V value) {
        if (Strings.isEmpty(key)) {
            throw new IllegalArgumentException("key");
        }
        if (value == null) {
            throw new IllegalArgumentException("value");
        }
        CharSequence seq = key;
        ContainmentIterator<V> iter = iterator();
        for (int i = 0; i < seq.length(); i++) {
            if (!iter.doContinue(seq.charAt(i))) {
                ((ContainmentIteratorImpl) iter).addStep(seq.charAt(i));
            }
        }
        iter.setValue(value);
    }


}
