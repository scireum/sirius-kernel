package sirius.kernel.commons;

/**
 * Helper class for working with double numbers.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/09
 */
public class Doubles {
    /**
     * Used as maximal epsilon (difference) when comparing numbers. Doubles are not compared using == due to numeric
     * constraints when using floating point numbers.
     */
    public static final double EPSILON = 0.000001d;

    /**
     * Returns the fractional part of the given value.
     * <p>
     * The fractional part is "everything behind the decimal separator". So for 1.454 it will be 0.454. This method
     * always returns an absolute value so the fractional part of -4.5656 will be 0.5656.
     *
     * @param val the number for return the fractional part of
     * @return the fractional part [0..1) of the number
     */
    public static double frac(double val) {
        return Math.abs(Math.abs(val) - Math.floor(Math.abs(val)));
    }

    /**
     * Determines if the given numbers are equal.
     * <p>
     * The numbers are not compared for absolute exactness (as == would do) but a minimal delta ({@link #EPSILON})
     * is permitted to make up for rounding errors which are in the nature of floating point numbers.
     *
     * @param a the first number to compare
     * @param b the second number to compare
     * @return <tt>true</tt> if the absolute difference of the given numbers is less than {@link #EPSILON},
     *         <tt>false</tt> otherwise
     */
    public static boolean areEqual(double a, double b) {
        return Math.abs(a - b) < EPSILON;
    }

    /**
     * Determines if the given number is zero (or very very close to).
     * <p>
     * Determines if the given number is zero or less than {@link #EPSILON} away from it. This is used to make up for
     * rounding errors which are in the nature of floating point numbers.
     *
     * @param val the number to check if it is 0.
     * @return <tt>true</tt> if the absolute value of the given numbers is less than {@link #EPSILON},
     *         <tt>false</tt> otherwise
     */
    public static boolean isZero(double val) {
        return Math.abs(val) < EPSILON;
    }
}
