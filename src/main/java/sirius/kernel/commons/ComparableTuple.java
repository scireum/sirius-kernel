/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Provides a tuple of values where the key is used as comparator.
 * <p>
 * Subclasses {@link Tuple} to implement <tt>Comparable</tt> based on the key, that is the first element of the tuple.
 * </p>
 *
 * @param <F> defines the first type of the tuple. The supplied class must implement <code>Comparable</code>
 * @param <S> defines the second type of the tuple
 * @author Andreas Haufler (aha@scireum.de)
 * @see Tuple
 * @see Comparable
 * @since 2013/08
 */
public class ComparableTuple<F extends Comparable<F>, S> extends Tuple<F, S> implements Comparable<ComparableTuple<F, S>> {

    /**
     * Creates a new tuple without any values.
     */
    @Nonnull
    public static <F extends Comparable<F>, S> ComparableTuple<F, S> createTuple() {
        return new ComparableTuple<F, S>(null, null);
    }

    /**
     * Creates a new tuple by only specifying the first value of the tuple.
     * <p>
     * The second value will remain <tt>null</tt>.
     * </p>
     *
     * @param first defines the first value of the tuple
     */
    @Nonnull
    public static <F extends Comparable<F>, S> ComparableTuple<F, S> create(@Nullable F first) {
        return new ComparableTuple<F, S>(first, null);
    }

    /**
     * Creates a new tuple with the given values.
     *
     * @param first  defines the first value of the tuple
     * @param second defines the second value of the tuple
     */
    @Nonnull
    public static <F extends Comparable<F>, S> ComparableTuple<F, S> create(@Nullable F first, @Nullable S second) {
        return new ComparableTuple<F, S>(first, second);
    }

    /**
     * Converts a map into a list of tuples.
     *
     * @param map the map to be converted
     * @param <K> the key type of the map and therefore the type of the first component of the tuples
     * @param <V> the value type of the map and therefore the type of the second component of the tuples
     * @return a list of tuples, containing one tuple per map entry where the first component is the key,
     *         and the second component is the value of the map entry.
     */
    public static <K extends Comparable<K>, V> List<ComparableTuple<K, V>> fromComparableMap(@Nonnull Map<K, V> map) {
        List<ComparableTuple<K, V>> result = new ArrayList<ComparableTuple<K, V>>(map.size());
        for (Map.Entry<K, V> e : map.entrySet()) {
            result.add(new ComparableTuple<K, V>(e.getKey(), e.getValue()));
        }
        return result;
    }

    /*
     * Internal constructor. Save a diamond an use the <tt>create</tt> methods.
     */
    protected ComparableTuple(F first, S second) {
        super(first, second);
    }


    @Override
    public int compareTo(ComparableTuple<F, S> o) {
        if (o == null) {
            return 1;
        }
        if (o.getFirst() == null && getFirst() != null) {
            return 1;
        } else if (getFirst() == null) {
            return 0;
        }
        if (getFirst() == null) {
            return -1;
        }
        return getFirst().compareTo(o.getFirst());
    }

}
