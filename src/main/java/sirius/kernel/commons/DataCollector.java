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
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Provides a pull pattern for asking methods to compute or fill a <tt>List</tt>.
 * <p>
 * When asking methods to create or populate a {@link List} it's easier to create and pass along a
 * <tt>Collector</tt> instead of having each method creating its own list and joining them afterwards.
 * <p>
 * By subclassing <tt>DataCollector</tt> one can also directly process the given values instead of just storing them
 * in a list.
 * <p>
 * A typical use-case is:
 * <pre>
 * {@code
 *             DataCollector&lt;String&gt; collector = new DataCollector&lt;String&gt;();
 *             computeStrings1(collector);
 *             computeStrings2(collector);
 *
 *             collector.getData(); // use values
 * }
 * </pre>
 * <p>
 * If a sorted list is required which does not depend on insertion order but on a given priority of each entry
 * {@link PriorityCollector} can be used.
 *
 * @param <T> the type of objects to be collected
 * @see PriorityCollector
 */
public class DataCollector<T> implements Consumer<T> {

    private List<T> data = new ArrayList<>();

    /**
     * Creates a new <tt>DataCollector</tt>.
     * <p>
     * Boilerplate method, so one doesn't need to re-type the type parameters.
     *
     * @param <T> the type of objects created by the data collector
     * @return a new <tt>DataCollector</tt>
     */
    public static <T> DataCollector<T> create() {
        return new DataCollector<>();
    }

    /**
     * Adds a value to the collector
     *
     * @param entity contains the value to be added to the collector.
     */
    public void add(T entity) {
        data.add(entity);
    }

    /**
     * Adds all values of the given collection to the collector
     *
     * @param entities the collection of values added to the collector.
     */
    public void addAll(@Nonnull Collection<? extends T> entities) {
        data.addAll(entities);
    }

    /**
     * Returns the <tt>List</tt> of values which where added to the collector so far.
     * <p>
     * For the sake of simplicity, this returns the internally used list. Therefore modifying this list, modifies
     * the collector.
     *
     * @return the list of values supplied to the collector so far.
     */
    @Nonnull
    public List<T> getData() {
        return data;
    }

    @Override
    public String toString() {
        return data.toString();
    }

    @Override
    public void accept(T t) {
        add(t);
    }
}
