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
 * Inserts the part registered in the <tt>GlobalContext</tt>. As lookup class, the type of the field is used.
 * <p>
 * This is the most common way to add a dependency to another part.
 * <p>
 * If a configPath is set, the system configuration is used to load the value specified for this path. The system
 * then tries to find the part with that name.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Part {

    /**
     * If set, the system config is used to determine the name used to load the part.
     *
     * @return the name of the system config key used to read the name of the part to load from
     */
    String configPath() default "";
}
