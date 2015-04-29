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
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2015/04
 */
public interface SubContext {

    /**
     * Gets notified if the associated CallContext is detached.
     */
    void detach();
}
