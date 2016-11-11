/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.transformers;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * A class implementing this interface supports the <b>Adapter Pattern</b>.
 * <p>
 * Using the {@link #as(Class)} method, the object can be casted or transformed to fulfill the requested type.
 */
public interface Transformable {

    /**
     * Determines if this can be transformed to the given <tt>type</tt>.
     *
     * @param type the target type to check for
     * @return <tt>true</tt> if this implements the type already or if can be transformed into the given
     * <tt>type</tt>
     */
    boolean is(@Nonnull Class<?> type);

    /**
     * Tries to transform this into the given <tt>adapterType</tt>.
     *
     * @param adapterType the target type to adapt to
     * @param <A>         the type of the adapter type
     * @return an optional which either contains the adapted value matching the requested <tt>adapterType</tt> or
     * an empty optional if no transformation was possible
     */
    @SuppressWarnings("unchecked")
    <A> Optional<A> tryAs(@Nonnull Class<A> adapterType);

    /**
     * Adapts this into the given <tt>adapterType</tt>.
     *
     * @param adapterType the target type to adapt to
     * @param <A>         the type of the adapter type
     * @return this adapted to match the requested <tt>adapterType</tt>
     * @throws IllegalArgumentException if no transformation was possible
     */
    <A> A as(@Nonnull Class<A> adapterType);
}
