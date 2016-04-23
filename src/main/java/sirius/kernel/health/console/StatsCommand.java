/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health.console;

import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.metrics.Metric;
import sirius.kernel.health.metrics.Metrics;

import javax.annotation.Nonnull;

/**
 * Console command which reports all available system metrics.
 */
@Register
public class StatsCommand implements Command {

    @Part
    private Metrics metrics;

    @Override
    public void execute(Output output, String... params) throws Exception {
        output.apply("%-8s %-30s %15s", "STATE", "NAME", "VALUE");
        output.separator();
        for (Metric metric : metrics.getMetrics()) {
            output.apply("%-8s %-30s %15s", metric.getState(), metric.getName(), metric.getValueAsString());
        }
        output.separator();
    }

    @Override
    @Nonnull
    public String getName() {
        return "stats";
    }

    @Override
    public String getDescription() {
        return "Reports all locally collected metrics of the system";
    }
}
