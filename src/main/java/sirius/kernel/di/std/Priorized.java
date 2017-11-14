/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.std;

/**
 * Marks a class as sortable by its priority.
 * <p>
 * Classes implementing this interface can be used by the {@link PriorityParts} annotation and will
 * be auto sorted (ascending) by priority before they are made available.
 */
@SuppressWarnings("squid:S1214")
public interface Priorized {

    /**
     * Contains the default priority used by <tt>Priorized</tt> parts.
     */
    int DEFAULT_PRIORITY = 100;

    /**
     * Returns the priority of this element.
     *
     * @return the priority - lower is better (comes first)
     */
    int getPriority();
}
