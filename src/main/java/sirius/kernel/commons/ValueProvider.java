/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

/**
 * Describes an interface which can generate or return a value when invoked.
 * <p>
 * Implementations can be passed along and then decide whether to lazily compute a value or to simply return a
 * previously set value.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public interface ValueProvider<T> {
    /**
     * Returns a value of type T
     *
     * @return returns a value of the designated type.
     */
    T get();
}
