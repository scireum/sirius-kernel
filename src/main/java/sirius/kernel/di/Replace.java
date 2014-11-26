/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di;

/**
 * Classes provided by customizations can be annotated with this to replace classes provided as a standard class.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2014/11
 */
public @interface Replace {

    /**
     * Contains the standard class to be replaced.
     *
     * @return the the standard class to be replaced
     */
    Class<?> value();

}


