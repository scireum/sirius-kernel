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
 * Can be applied to fields to insert the current {@link sirius.kernel.di.GlobalContext}.
 * <p>
 * Can be used to access the <tt>GlobalContext</tt> to wire other objects or to lookup a named part via
 * {@link sirius.kernel.di.GlobalContext#findPart(String, Class)}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Context {
}
