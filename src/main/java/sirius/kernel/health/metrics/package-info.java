/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

/**
 * Used to record and monitor system metrics.
 * <p>
 * Defines a discovery interface {@link sirius.kernel.health.metrics.MetricProvider} which is found and monitored
 * by {@link sirius.kernel.health.metrics.Metrics}. The {@link sirius.kernel.health.metrics.SystemMetricProvider}
 * uses <b>Sigar</b> and JMX to provide some basic metrics for the JVM and the underlying machine.
 * <p>
 * Limits for all metrics can be defined in the system configuration under <tt>health.limits.[metric]</tt>:
 * <ul>
 *     <li>
 *         <tt>gray</tt>: Up to this value, it is considered as not noteworthy and normally not shown.
 *         Everything above is active, but good.
 *     </li>
 *     <li>
 *         <tt>warning</tt>: If this value is reached, this metric is considered increased but still acceptable.
 *     </li>
 *     <li><tt>error</tt>: Once this value is reached, the system stability is no longer guaranteed and some action
 *     is required. If there is no upper limit for a metric, this value can also be skipped.
 * </ul>
 * <p>
 * Eache module which provides metrics normally also defines some default limits in its <tt>component-[name].conf</tt>.
 */
        package sirius.kernel.health.metrics;