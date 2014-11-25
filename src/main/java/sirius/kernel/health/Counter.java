/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health;

import java.util.concurrent.TimeUnit;

/**
 * Represents a counter for statistical use. Overflows to 0 instead to {@link Long#MIN_VALUE}
 * <p>
 * Counts up to <code>Long.MAX_VALUE - 1</code> starting at 0 and overflowing to 0.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public class Counter {
    private volatile long startTimeMillis = -1;
    private volatile long count = 0;
    private final long max;

    /**
     * Creates a new counter with the given max. value
     *
     * @param max maximal value which will force to counter to overflow and reset to 0
     */
    public Counter(long max) {
        this.max = max;
    }

    /**
     * Creates a new counter up to <code>Long.MAX.VALUE - 1</code>
     */
    public Counter() {
        this(Long.MAX_VALUE - 1);
    }

    /**
     * Increments the counter by one
     */
    public long inc() {
        if (startTimeMillis < 0) {
            reset();
        }
        if (count < max) {
            count++;
        } else {
            count = 0;
        }
        return count;
    }

    /**
     * Computes the average raise per given unit of time.
     *
     * @param unit the unit of time to compute the average for.
     * @return the average increment per given time unit
     */
    public double getAvgPer(TimeUnit unit) {
        return (count) / getDuration(unit);
    }

    /**
     * Returns the current value of the counter
     *
     * @return the value of the counter
     */
    public long getCount() {
        return count;
    }

    /**
     * Returns the time since the counter was created.
     *
     * @param unit the time unit used for the return value
     * @return the time since the creation of the counter in the given time unit
     */
    public long getDuration(TimeUnit unit) {
        long delta = System.currentTimeMillis() - startTimeMillis;
        return TimeUnit.MILLISECONDS.convert(delta, unit);
    }

    /**
     * Resets the counter to zero
     */
    public void reset() {
        startTimeMillis = System.currentTimeMillis();
        count = 0;
    }

    @Override
    public String toString() {
        return String.valueOf(count);
    }
}
