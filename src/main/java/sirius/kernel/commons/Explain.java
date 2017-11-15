/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Can be used in conjuncion with {@link SuppressWarnings} to provide an explanation.
 * <p>
 * This should be used to provide an explanation when findbugs or other inspections are suppressed.
 */
@Retention(RetentionPolicy.CLASS)
public @interface Explain {

    /**
     * Provides a brief explanation why a certain inspection is suppressed.
     *
     * @return the explanation why an inspection is suppressed
     */
    String value();
}
