/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.morphium;

import sirius.kernel.commons.Strings;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * A class implementing this interface supports the <b>Adapter Pattern</b>.
 * <p>
 * Using the {@link #as(Class)} method, the object can be casted or adapted to fulfill the requested  type. By
 * default the object is either casted (if possible) or
 * {@link sirius.kernel.di.morphium.Adapters#make(Adaptable, Class)} is invoked appropriately. Classes implementing
 * this interface may choose to override this behaviour.
 * <p>
 * So where might an adapter pattern be more useful than explicitly declaring interfaces? One use case are framework
 * classes or interfaces which might be extended by a consumer. If this class or interface derives from
 * <tt>Adaptable</tt>, the extended functionality can be easily accessed (with full type safety) where the framework
 * class or interface is available.
 */
public interface Adaptable {

    /**
     * Determines if this can be adapted to the given <tt>type</tt>.
     *
     * @param type the target type to check for
     * @return <tt>true</tt> if this implements the type already or if an adapter is present to make this matching
     * <tt>type</tt>
     */
    default boolean is(@Nonnull Class<?> type) {
        if (type.isInstance(this)) {
            return true;
        }
        return Adapters.canMake(this, type);
    }

    /**
     * Tries to adapt this into the given <tt>adapterType</tt>.
     *
     * @param adapterType the target type to adapt to
     * @param <A>         the type of the adapter type
     * @return an optional which either contains the adapted value matching the requested <tt>adapterType</tt> or
     * an empty optional if no transformation was possible
     */
    @SuppressWarnings("unchecked")
    default <A> Optional<A> tryAs(@Nonnull Class<A> adapterType) {
        if (adapterType.isInstance(this)) {
            return Optional.of((A) this);
        }
        return Optional.ofNullable(Adapters.make(this, adapterType));
    }

    /**
     * Adapts this into the given <tt>adapterType</tt>.
     *
     * @param adapterType the target type to adapt to
     * @param <A>         the type of the adapter type
     * @return this adapted to match the requested <tt>adapterType</tt>
     * @throws IllegalArgumentException if no transformation was possible
     */
    default <A> A as(@Nonnull Class<A> adapterType) {
        Optional<A> result = tryAs(adapterType);
        if (result.isPresent()) {
            return result.get();
        } else {
            throw new IllegalArgumentException(Strings.apply("Cannot morph %s into %s",
                                                             getClass().getName(),
                                                             adapterType.getName()));
        }
    }
}
