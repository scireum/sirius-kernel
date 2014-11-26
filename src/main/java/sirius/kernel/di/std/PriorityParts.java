package sirius.kernel.di.std;

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
 * @author Andreas Haufler (aha@scireum.de)
 * @see Parts
 * @since 2013/08
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)
public @interface PriorityParts {

    /**
     * Determines the lookup class used to retrieve the parts from the <tt>GlobalContext</tt>.
     *
     * @return the lookup class used to fetch all parts of interest.
     */
    Class<? extends Priorized> value();
}
