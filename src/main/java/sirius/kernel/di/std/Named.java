/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.std;

import javax.annotation.Nonnull;

/**
 * Represents a named service.
 * <p>
 * Classes implementing this interface will automatically use the value returned by <tt>getName()</tt> as name.
 * Therefore a simple {@link sirius.kernel.di.std.Register} annotation is sufficient without the name parameter
 * filled.
 */
public interface Named {
    /**
     * Returns the name of the part.
     * <p>
     * The return value is used as part name when the <tt>Register</tt> annotation is processed.
     *
     * @return the name of this part
     */
    @Nonnull
    String getName();
}
