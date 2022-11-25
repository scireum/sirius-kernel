/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health.metrics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Used by implementations of {@link MetricProvider} to provide metrics to {@link Metrics}.
 */
public interface MetricsCollector {

    /**
     * Provides a metric which state is interpreted using the config values a defined by <tt>limitType</tt>.
     * <p>
     * Therefore if the given value is less than <tt>health.limits.&lt;limitType&gt;.gray</tt> if will be GRAY.
     * Respectively <tt>health.limits.&lt;limitType&gt;.warning</tt> and <tt>health.limits.&lt;limitType&gt;.error</tt>
     * will be used to determine if it is GREEN, YELLOW or RED. If either of the config values is missing or 0, it will
     * be ignored.
     *
     * @param code      the unique id used for the metric
     * @param limitType the name used to retrieve the limits from the system config
     * @param label     the name of the metric
     * @param value     the measured value of the metric
     * @param unit      the unit used by value. Can be <tt>null</tt> if there is no unit. Preferably, use one of the
     *                  units provided in {@link Metric}
     */
    void metric(@Nonnull String code,
                @Nonnull String limitType,
                @Nonnull String label,
                double value,
                @Nullable String unit);

    /**
     * Provides a metric using the given values.
     *
     * @param code  the unique id used for the metric
     * @param label the name of the metric
     * @param value the measured value of the metric
     * @param unit  the unit used by value. Can be <tt>null</tt> if there is no unit. Preferably, use one of the
     *              units provided in {@link Metric}
     * @param state the interpretation of the measured value
     */
    void metric(@Nonnull String code, String label, double value, String unit, MetricState state);

    /**
     * Provides a differential metric.
     * <p>In contrast to {@link #metric(String, String, String, double, String)} this will not report the absolute value
     * but the difference to the last measured value. This value will be stored inside a special map, hence an unique
     * id is required to identify it.
     *
     * @param code         the unique id used for the metric
     * @param limitType    the name used to fetch the limits from the system configuration
     * @param label        the name of the metric
     * @param currentValue the current value
     * @param unit         the unit used to measure the difference between the last and the current value.
     *                     Preferably, use one of the units provided in {@link Metric}
     */
    void differentialMetric(String code, @Nonnull String limitType, String label, double currentValue, String unit);
}
