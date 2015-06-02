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
 * Can be used to require the presence of a framework when processing class load actions.
 * <p>
 * Note that the {@link Register} annotation itself as ({@link Register#framework()} to specify
 * the framework required. So this annotation is intended for custom class load actions.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Framework {
    /**
     * Determines the framework this part belongs to. If a non empty string is given, the part is only registered, if
     * {@link sirius.kernel.Sirius#isFrameworkEnabled(String)} returns <tt>true</tt> for the given framework.
     *
     * @return the name of the framework which has to be enabled in order for this annotation to become active
     */
    String value();
}
