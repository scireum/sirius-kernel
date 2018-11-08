/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.cache;

import sirius.kernel.Stoppable;
import sirius.kernel.di.std.Register;

/**
 * Clears the caches when Sirius is shutting down
 */
@Register
public class Caches implements Stoppable {

    @Override
    public void stopped() {
        CacheManager.reset();
    }
}
