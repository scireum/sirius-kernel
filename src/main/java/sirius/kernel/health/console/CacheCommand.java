/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health.console;

import sirius.kernel.cache.Cache;
import sirius.kernel.cache.CacheManager;
import sirius.kernel.commons.Value;
import sirius.kernel.di.std.Register;

import javax.annotation.Nonnull;

/**
 * Console command which reports statistics for all caches.
 */
@Register
public class CacheCommand implements Command {

    @Override
    public void execute(Output output, String... params) {
        if (params.length > 0) {
            for (Cache<?, ?> c : CacheManager.getCaches()) {
                if (Value.indexOf(0, params).asString().equals(c.getName())) {
                    output.apply("Flushing: %s", params[0]);
                    c.clear();
                }
            }
        } else {
            output.line("Use cache <name> to flush the given cache...");
        }
        output.blankLine();
        output.apply("%-53s %8s %8s %8s", "NAME", "SIZE", "MAX-SIZE", "HIT-RATE");
        output.separator();
        for (Cache<?, ?> c : CacheManager.getCaches()) {
            output.apply("%-53s %8d %8d %8d", c.getName(), c.getSize(), c.getMaxSize(), c.getHitRate());
        }
        output.separator();
    }

    @Override
    @Nonnull
    public String getName() {
        return "cache";
    }

    @Override
    public String getDescription() {
        return "Lists all available caches.";
    }
}
