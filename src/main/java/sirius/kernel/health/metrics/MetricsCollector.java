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
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2014/01
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
     * @param limitType the name used to retrieve the limits from the system config
     * @param title     the name of the metric
     * @param value     the measured value of the metric
     * @param unit      the unit used by value. Can be <tt>null</tt> if there is no unit
     */
    void metric(@Nonnull String limitType, @Nonnull String title, double value, @Nullable String unit);

    /**
     * Provides a metric using the given values.
     *
     * @param title the name of the metric
     * @param value the measured value of the metric
     * @param unit  the unit used by value. Can be <tt>null</tt> if there is no unit
     * @param state the interpretation of the measured value
     */
    void metric(String title, double value, String unit, MetricState state);

    /**
     * Provides a differential metric.
     * <p>In contrast to {@link #metric(String, String, double, String)} this will not report the absolute value
     * but the difference to the last measured value. This value will be stored inside a special map, hence an unique
     * id is required to identify it.
     *
     * @param id           the unique id used to store and retrieve the last measured value
     * @param limitType    the name used to fetch the limits from the system configuration
     * @param title        the name of the metric
     * @param currentValue the current value
     * @param unit         the unit used to measure the difference between the last and the current value
     */
    void differentialMetric(String id, String limitType, String title, double currentValue, String unit);
}
