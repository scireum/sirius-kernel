/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.std;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Inserts all parts registered in the <tt>GlobalContext</tt> as <tt>List</tt>.
 * <p>
 * The references class must be an implementation of {@link Priorized} and all parts will
 * be sorted by their priority (ascending) before the list is inserted.
 *
 * @see Parts
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PriorityParts {

    /**
     * Determines the lookup class used to retrieve the parts from the <tt>GlobalContext</tt>.
     *
     * @return the lookup class used to fetch all parts of interest.
     */
    Class<? extends Priorized> value();
}
