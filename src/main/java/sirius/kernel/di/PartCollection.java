/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * Represents a collection, which always contains all registered parts for the
 * given interface (PartCollection{@link #getInterface()}.
 * <p>
 * This is the content of a field wearing the {@link sirius.kernel.di.std.Parts} annotation.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public interface PartCollection<P> extends Iterable<P> {

    /**
     * Returns the class which is used to fetch parts from the {@link GlobalContext}
     *
     * @return the class which is used to determine which parts should be in this collection.
     */
    @Nonnull
    Class<P> getInterface();

    /**
     * Returns all parts currently registered for the given class.
     *
     * @return all parts in the {@link GlobalContext} which were registered for the given class. If no parts are found,
     *         and empty collection is returned.
     */
    @Nonnull
    Collection<P> getParts();
}
