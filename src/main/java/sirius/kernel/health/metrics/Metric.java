/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health.metrics;

import sirius.kernel.commons.Amount;
import sirius.kernel.commons.NumberFormat;

/**
 * Represents a measured value, recorded by the metrics framework.
 * <p>
 * Basically this combines a name along with a value and unit. Additionally an interpretation of the value is
 * given as state.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/09
 */
public class Metric implements Comparable<Metric> {

    private final String unit;
    private String name;
    private double value;
    private MetricState state;

    /**
     * Creates a new metric using the given values
     *
     * @param name  the name of the metric
     * @param value the actual value
     * @param state the interpretation of the value
     * @param unit  the unit in which the value is measured or <tt>null</tt> if there is no unit
     */
    public Metric(String name, double value, MetricState state, String unit) {
        this.unit = unit;
        this.name = name;
        this.value = value;
        this.state = state;
    }

    /**
     * Returns the name of the metric.
     *
     * @return the name of the metric
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the actual value of the metric.
     *
     * @return the measured value of the metric
     */
    public double getValue() {
        return value;
    }

    /**
     * The unit in which the value is measured
     *
     * @return the unit of the value or <tt>null</tt> if there is no unit
     */
    public String getUnit() {
        return unit;
    }

    /**
     * String representation of the value along with its unit (is necessary)
     *
     * @return a string representation of the value
     */
    public String getValueAsString() {
        return Amount.of(value).toSmartRoundedString(NumberFormat.TWO_DECIMAL_PLACES).append(" ", unit).toString();
    }

    /**
     * Returns an interpretation of the value (@see MetricState).
     *
     * @return an interpretation of the value
     */
    public MetricState getState() {
        return state;
    }

    @Override
    public int compareTo(Metric o) {
        if (o == null) {
            return -1;
        }
        if (o == this) {
            return 0;
        }
        if (o.state != state) {
            return o.state.ordinal() - state.ordinal();
        }
        return name.compareTo(o.name);
    }
}
