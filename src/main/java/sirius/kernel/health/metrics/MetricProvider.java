/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health.metrics;

/**
 * Provides metrics to the metrics system.
 * <p>
 * Instances of this class can be registered using the {@link sirius.kernel.di.std.Register} annotation which then
 * can provide metrics for various areas of the system.
 */
public interface MetricProvider {

    /**
     * Invoked roughly every minute to collect all available metrics.
     *
     * @param collector the interface used to provide metrics
     */
    void gather(MetricsCollector collector);
}
