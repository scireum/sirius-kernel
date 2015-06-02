/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.std;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Inserts all parts registered in the <tt>GlobalContext</tt>, either as <tt>Collection</tt> or as
 * {@link sirius.kernel.di.PartCollection}.
 * <p>
 * This is the central extension mechanism to permit other modules to add functionality to the current one, as yet
 * unknown classes can be registered for the given lookup class. Using this yields in loose coupling and extensible
 * software design.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Parts {

    /**
     * Determines the lookup class used to retrieve the parts from the <tt>GlobalContext</tt>.
     *
     * @return the lookup class used to fetch all parts of interest.
     */
    Class<?> value();
}
