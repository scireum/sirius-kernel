package sirius.kernel.di.std;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Can be applied to fields to insert the current {@link sirius.kernel.di.GlobalContext}.
 * <p>
 * Can be used to access the <tt>GlobalContext</tt> to wire other objects or to lookup a named part via
 * {@link sirius.kernel.di.GlobalContext#findPart(String, Class)}.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)
public @interface Context {
}
