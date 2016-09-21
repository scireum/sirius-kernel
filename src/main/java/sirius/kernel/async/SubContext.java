/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

/**
 * Represents a sub context which can be managed by {@link CallContext}.
 */
public interface SubContext {

    /**
     * Returns an instance which is used in a forked {@link CallContext}.
     *
     * @return either the instance itself - if can or has to be used in both threads - or clone or copy of the instance
     * which replicates the current state
     */
    SubContext fork();

    /**
     * Gets notified if the associated CallContext is detached.
     */
    void detach();
}
