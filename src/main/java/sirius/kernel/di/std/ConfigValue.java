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
 * Reads the config value given in <tt>value</tt> and inserts in into the field, wearing this annotation.
 * <p>
 * Provides a shortcut for accessing the config ({@link sirius.kernel.Sirius#getConfig()}). Also it performs the
 * appropriate conversion to the target type of the given field. A default value should be placed in the
 * component config (which then can be overridden in the application.conf or instance.conf).
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)
public @interface ConfigValue {

    /**
     * Contains the dot separated path to the desired config value.
     *
     * @return the path for the value to fetch from the config
     */
    String value();

    /**
     * Determines if the value is required (will cause an error if not filled).
     * <p>
     * Basically this should not happen, as it is a good practice to place a default value in the current
     * component config.
     * </p>
     *
     * @return <tt>true</tt> if the value must be filled, <tt>false</tt> (default) otherwise.
     */
    boolean required() default false;
}
