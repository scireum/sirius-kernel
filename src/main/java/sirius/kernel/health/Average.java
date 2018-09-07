/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health;

import com.google.common.util.concurrent.AtomicDouble;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents an average value over a given set of values.
 * <p>
 * Using the <tt>maxSamples</tt> can be used to implement something like a sliding avarage as the value is
 * reset (computed) once the counter hits <tt>maxSamples</tt>.
 */
public class Average {

    private static final long DEFAULT_MAX_SAMPLES = 100;
    private AtomicLong sampleCount = new AtomicLong();
    private AtomicLong count = new AtomicLong();
    private AtomicDouble sum = new AtomicDouble();
    private final long maxSamples;

    /**
     * Creates a new average which averages up to {@link #DEFAULT_MAX_SAMPLES} and then computes the effective
     * avarage and resets the counter to 1 using that value.
     */
    public Average() {
        this(DEFAULT_MAX_SAMPLES);
    }

    /**
     * Creates a new average which averages up to <tt>maxSamples</tt> and then computes the effective
     * avarage and resets the counter to 1 using that value.
     *
     * @param maxSamples the max number of samples to use in order to build the average
     */
    public Average(long maxSamples) {
        this.maxSamples = maxSamples;
    }

    /**
     * Adds the given value to the set of values on which the average is based.
     * <p>
     * If the sum of all values is greater as <tt>Double.MAX_VALUE / 2</tt> or the count of all values is greater as
     * <tt>Long.Max_VALUE / 2</tt>, the average is resetted.
     *
     * @param value to value to add to the average
     */
    public void addValue(long value) {
        addValue((double) value);
    }

    /**
     * Adds the given value to the set of values on which the average is based.
     * <p>
     * If the sum of all values is greater as <tt>Double.MAX_VALUE / 2</tt> or the count of all values is greater as
     * <tt>Long.Max_VALUE / 2</tt>, the average is resetted.
     *
     * @param value to value to add to the average
     */
    public void addValue(double value) {
        addValues(1, value);
    }

    /**
     * Adds the given number of values to the counter and increments the sum by the given delta.
     *
     * @param numberOfValues the number of values to add
     * @param sumOfValue     the total sum of the values to add
     */
    public void addValues(long numberOfValues, double sumOfValue) {
        long newSampleCount = sampleCount.addAndGet(numberOfValues);
        double newSum = sum.addAndGet(sumOfValue);

        if (Long.MAX_VALUE - count.get() < numberOfValues) {
            count.set(numberOfValues);
        } else {
            count.addAndGet(numberOfValues);
        }

        if (newSampleCount >= maxSamples || newSum > Double.MAX_VALUE / 2) {
            sampleCount.set(maxSamples / 2);
            sum.set(newSum / newSampleCount * sampleCount.get());
        }
    }

    /**
     * Returns the average of the added values.
     * <p>
     * Returns the sliding average of the last 100 values
     *
     * @return the average of the added values
     */
    public double getAvg() {
        long counter = sampleCount.get();
        if (counter == 0) {
            return 0d;
        }

        return sum.get() / counter;
    }

    /**
     * Returns the average just like {@link #getAvg()} but then resets the internal buffers to zero.
     *
     * @return the average of the last 100 values
     */
    public double getAndClear() {
        double avg = getAvg();
        count.set(0);
        sampleCount.set(0);
        sum.set(0);
        return avg;
    }

    /**
     * Returns the number of values used to compute the average.
     *
     * @return the number of value which will be considered when computing the average.
     */
    public long getSampleCount() {
        return sampleCount.get();
    }

    /**
     * Returns the number of total values inserted in the average.
     *
     * @return the number of total values inserted in the average
     */
    public long getCount() {
        return count.get();
    }

    @Override
    public String toString() {
        return getAvg() + " (" + count + ")";
    }
}
