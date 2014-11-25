package sirius.kernel.di.std;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Inserts the part registered in the <tt>GlobalContext</tt>. As lookup class, the type of the field is used.
 * <p>
 * This is the most common way to add a dependency to another part.
 * </p>
 * <p>
 * If a configPath is set, the system configuration is used to load the value specified for this path. The system
 * then tries to find the part with that name.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)
public @interface Part {
    String configPath() default "";
}
