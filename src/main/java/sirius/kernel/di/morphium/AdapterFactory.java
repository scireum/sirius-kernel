/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.morphium;

/**
 * Generates adapter instances for a given type.
 * <p>
 * Used by {@link sirius.kernel.di.morphium.Adapters} to generate adapters of a certain type for a given input. This
 * permits to transform existing classes into interfaces or other classes without modifying them.
 *
 * @param <A> the type of objects created by this adapter.
 */
public interface AdapterFactory<A> {

    /**
     * Returns the target type for which this factory can create adapters.
     *
     * @return the target type for which adapters are created
     */
    Class<A> getAdapterClass();

    /**
     * Generates a new object of the desired target type for the given object to adapt.
     *
     * @param adaptable the object to adapt
     * @return the adapted instance (matching the target class) or <tt>null</tt> to indicate that no conversion
     * was possible
     */
    A make(Adaptable adaptable);
}
