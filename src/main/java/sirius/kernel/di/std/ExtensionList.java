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
 * Can be applied to fields to insert the denoted list of extensions defined in the system config.
 * <p>
 * This is a shortcut for {@link sirius.kernel.extensions.Extensions#getExtensions(String)}.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @see sirius.kernel.extensions.Extensions
 * @since 2013/08
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)
public @interface ExtensionList {

    /**
     * Defines the type of extensions to be fetched.
     *
     * @return a dot separated path defining the config-map to load the extensions from.
     */
    String value();

}
