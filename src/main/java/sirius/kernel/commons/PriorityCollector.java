/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides a pull pattern just like {@link DataCollector} with an externally supplied order.
 * <p>
 * When asking methods to create or populate a {@link List} it's easier to create and pass along a
 * <tt>PriorityCollector</tt> instead of having each method creating its own list and joining them afterwards.
 * </p>
 * <p>
 * Using a <tt>PriorityCollector</tt>, several methods can be supplied with the same instance and generate a list or a
 * custom (externally) ordering. This greatly simplifies creating extensible systems which are enhanced by
 * sub-components and where order of elements matter. The final list is sorted by comparing the priority.
 * Therefore if <code>a.priority &lt; b.priority</code> then <tt>a</tt> will occur before <tt>b</tt> in the list.
 * </p>
 * <p>
 * If the order of the provided elements does not matter, a {@link DataCollector} can be used.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @see DataCollector
 * @since 2013/08
 */
public class PriorityCollector<T> {

    /**
     * Creates a new <tt>PriorityCollector</tt>.
     * <p>
     * Boilerplate method, so one doesn't need to re-type the type parameters.
     * </p>
     *
     * @return a new <tt>PriorityCollector</tt>
     */
    public static <T> PriorityCollector<T> create() {
        return new PriorityCollector<T>();
    }

    /**
     * Provides a default constant which can be used if a collector is pre-populated with standard values and then
     * enhanced by sub modules.
     * <p>
     * Using an agreed upon standard value makes it easy for component creators to provide values which will be
     * inserted before or after the default value.
     * </p>
     */
    public static final int DEFAULT_PRIORITY = 100;

    protected List<ComparableTuple<Integer, T>> data = new ArrayList<ComparableTuple<Integer, T>>();

    /**
     * Adds the given entity with the given priority.
     *
     * @param priority the priority to use for this element.
     * @param entity   the entity added to the collector.
     */
    public void add(int priority, T entity) {
        data.add(new ComparableTuple<Integer, T>(priority, entity));
    }

    /**
     * Adds the given entity with the default priority.
     *
     * @param entity the entity added to the collector.
     * @see #DEFAULT_PRIORITY
     */
    public void addDefault(T entity) {
        data.add(new ComparableTuple<Integer, T>(DEFAULT_PRIORITY, entity));
    }

    /**
     * Returns the list of added entities sorted by priority.
     * <p>
     * The comparator used is &lt; - therefore if <code>a.priority &lt; b.priority</code>
     * then <tt>a</tt> will occur before <tt>b</tt> in the list.
     * </p>
     *
     * @return the list of entities ordered by priority ascending
     */
    public List<T> getData() {
        Collections.sort(data);
        return Tuple.seconds(data);
    }

    /**
     * Returns the number of items added to this collector.
     *
     * @return the number of items in this collector
     */
    public int size() {
        return data.size();
    }

    @Override
    public String toString() {
        return data.toString();
    }
}
