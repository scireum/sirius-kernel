/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.cache;

import sirius.kernel.async.Tasks;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Register;
import sirius.kernel.timer.EveryTenMinutes;

/**
 * Invoked regularly to remove outdated entries from the system caches.
 */
@Register
public class CacheEvictionTimer implements EveryTenMinutes {

    @Part
    private Tasks tasks;

    @Override
    public void runTimer() throws Exception {
        tasks.defaultExecutor().start(this::runEviction);
    }

    private void runEviction() {
        for (Cache<?, ?> cache : CacheManager.getCaches()) {
            CacheManager.LOG.FINE("Running cache eviction for: %s", cache.getName());
            cache.updateStatistics();
            cache.runEviction();
        }
    }
}
