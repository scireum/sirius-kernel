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
 * Created by aha on 24.11.14.
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
            throw new IllegalArgumentException(Strings.apply("Cannot morph %s into %s", getClass().getName(), adapterType.getName()));
        }
    }

}
