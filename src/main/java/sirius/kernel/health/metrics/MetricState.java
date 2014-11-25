/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health.metrics;

/**
 * Represents an interpretation of a measured metric.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2014/01
 */
public enum MetricState {
    /**
     * Indicates an "uninteresting" value. These are values like "0" or of very low activity.
     */
    GRAY,
    /**
     * Indicates an "interesting" amount of activity
     */
    GREEN,
    /**
     * Indicates a critical amount of activity
     */
    YELLOW,
    /**
     * Indicates a failure condition (system is failing or very likely to fail soon)
     */
    RED;
}
