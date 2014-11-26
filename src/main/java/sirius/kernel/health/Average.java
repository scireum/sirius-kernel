/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health;

/**
 * Represents an average value over a given set of values.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public class Average {

    private long count = 0;
    private long[] values = new long[100];
    private int index = 0;
    private int filled = 0;

    /**
     * Adds the given value to the set of values on which the average is based.
     * <p>
     * If the sum of all values is greater as <tt>Long.MAX_VALUE</tt> or the count of all values is greater as
     * <tt>Long.Max_VALUE</tt>, the value is ignored.
     *
     * @param value to value to add to the average
     */
    public void addValue(long value) {
        synchronized (values) {
            values[index++] = value;
            if (index > filled) {
                filled = index;
            }
            if (index >= values.length - 1) {
                index = 0;
            }
        }
        if (count >= Long.MAX_VALUE - 1) {
            count = 0;
        }
        count++;
    }

    /**
     * Returns the average of the added values.
     * <p>
     * Returns the sliding average of the last 100 values
     *
     * @return the average of the added values
     */
    public double getAvg() {
        if (filled == 0) {
            return 0.0d;
        }
        double result = 0.0d;
        double min = Double.NaN;
        double max = Double.NaN;
        for (int i = 0; i <= filled; i++) {
            result += values[i];
            if (filled >= 5) {
                if (Double.isNaN(min) || values[i] < min) {
                    min = values[i];
                }
                if (Double.isNaN(max) || values[i] > max) {
                    max = values[i];
                }
            }
        }

        // If we have five or more samples, we smooth the result by dropping the lowest and highest value
        if (filled >= 5) {
            result -= min + max;
            return result / (filled - 2.0d);
        }
        return result / (double) filled;
    }

    /**
     * Returns the average just like {@link #getAvg()} but then resets the internal buffer to zero.
     * <p>
     * Note that the internal value counter won't be reset. Only the buffer containing the last 100
     * measurements will be reset.
     *
     * @return the average of the last 100 values
     */
    public double getAndClearAverage() {
        double avg = getAvg();
        filled = 0;
        return avg;
    }

    /**
     * Returns the number of values used to compute the average.
     *
     * @return the number of value which will be considered when computing the average.
     */
    public long getCount() {
        return count;
    }

    @Override
    public String toString() {
        return String.valueOf(getAvg()) + " (" + count + ")";
    }
}
