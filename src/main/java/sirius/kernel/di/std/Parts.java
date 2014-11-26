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
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)
public @interface Parts {

    /**
     * Determines the lookup class used to retrieve the parts from the <tt>GlobalContext</tt>.
     *
     * @return the lookup class used to fetch all parts of interest.
     */
    Class<?> value();
}
