/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.cache;

import sirius.kernel.di.std.Register;
import sirius.kernel.timer.EveryTenMinutes;

/**
 * Invoked regularly to remove outdated entries from the system caches
 * <p>
 * This class is responsible to run the cache evictions of all known caches.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
@Register
public class CacheEvictionTimer implements EveryTenMinutes {

    @Override
    public void runTimer() throws Exception {
        for (Cache<?, ?> cache : CacheManager.getCaches()) {
            CacheManager.LOG.FINE("Running cache eviction for: %s", cache.getName());
            cache.runEviction();
        }

    }

}
