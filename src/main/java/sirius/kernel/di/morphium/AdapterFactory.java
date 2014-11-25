/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.morphium;

/**
 * Created by aha on 24.11.14.
 */
public interface AdapterFactory<A> {

    Class<A> getAdapterClass();

    A make(Adaptable adaptable);

}
