/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a counter for statistical use. Overflows to 0 instead to {@link Long#MIN_VALUE}
 * <p>
 * Counts up to {@code Long.MAX_VALUE - 1} starting at 0 and overflowing to 0.
 */
public class Counter {
    private AtomicLong startTimeMillis = new AtomicLong(System.currentTimeMillis());
    private AtomicLong count = new AtomicLong();
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
     * Creates a new counter up to {@code Long.MAX.VALUE - 1}
     */
    public Counter() {
        this(Long.MAX_VALUE - 1);
    }

    /**
     * Increments the counter by one
     *
     * @return the update value of the counter
     */
    public long inc() {
        return add(1);
    }

    /**
     * Adds the given delta to the counter.
     *
     * @param delta the delta to add
     * @return the update value of the counter
     */
    public long add(long delta) {
        long result = count.addAndGet(delta);
        if (result >= max) {
            count.set(0);
        }

        return result;
    }

    /**
     * Computes the average raise per given unit of time.
     *
     * @param unit the unit of time to compute the average for.
     * @return the average increment per given time unit
     */
    public double getAvgPer(TimeUnit unit) {
        return (double) count.get() / getDuration(unit);
    }

    /**
     * Returns the current value of the counter
     *
     * @return the value of the counter
     */
    public long getCount() {
        return count.get();
    }

    /**
     * Returns the time since the counter was created.
     *
     * @param unit the time unit used for the return value
     * @return the time since the creation of the counter in the given time unit
     */
    public long getDuration(TimeUnit unit) {
        long delta = System.currentTimeMillis() - startTimeMillis.get();
        return unit.convert(delta, TimeUnit.MILLISECONDS);
    }

    /**
     * Resets the counter to zero
     */
    public void reset() {
        startTimeMillis.set(System.currentTimeMillis());
        count.set(0);
    }

    @Override
    public String toString() {
        return String.valueOf(count);
    }
}
