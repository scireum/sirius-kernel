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
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2015/01
 */
public interface Adaptable {

    default boolean is(@Nonnull Class<?> type) {
        if (type.isInstance(this)) {
            return true;
        }
        return Adapters.canMake(this, type);
    }

    @SuppressWarnings("unchecked")
    default <A> Optional<A> tryAs(@Nonnull Class<A> adapterType) {
        if (adapterType.isInstance(this)) {
            return Optional.of((A) this);
        }
        return Optional.ofNullable(Adapters.make(this, adapterType));
    }

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
