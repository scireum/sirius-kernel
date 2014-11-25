/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import sirius.kernel.nls.NLS;

import java.math.RoundingMode;
import java.text.DecimalFormatSymbols;

/**
 * Used to define the parameters used to create a string representation of a number.
 * <p>
 * Provides a set of default formats and also describes the parameters used to format a number. This is used
 * by {@link Amount} to create string representations.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public interface NumberFormat {

    /**
     * Describes the default format used to create string representations of percentages.
     * <p>
     * It therefore specifies two decimal places which are rounded {@link RoundingMode#HALF_UP}. It uses
     * the decimal format symbols for the currently active language provided by
     * {@link sirius.kernel.nls.NLS}. As suffix the percent sign <tt>%</tt> is used.
     * </p>
     *
     * @see sirius.kernel.nls.NLS#getDecimalFormatSymbols()
     */
    public static final NumberFormat PERCENT = new NumberFormat() {

        @Override
        public String getSuffix() {
            return "%";
        }

        @Override
        public int getScale() {
            return 2;
        }

        @Override
        public RoundingMode getRoundingMode() {
            return RoundingMode.HALF_UP;
        }

        @Override
        public DecimalFormatSymbols getDecimalFormatSymbols() {
            return NLS.getDecimalFormatSymbols();
        }

        @Override
        public String toString() {
            return "PERCENT";
        }
    };

    /**
     * Describes a format which round to two decimal places.
     * <p>
     * It specifies {@link RoundingMode#HALF_UP} as rounding mode and uses
     * the decimal format symbols for the currently active language provided by
     * {@link sirius.kernel.nls.NLS}.
     * </p>
     *
     * @see sirius.kernel.nls.NLS#getDecimalFormatSymbols()
     */
    public static final NumberFormat TWO_DECIMAL_PLACES = new NumberFormat() {

        @Override
        public String getSuffix() {
            return null;
        }

        @Override
        public int getScale() {
            return 2;
        }

        @Override
        public RoundingMode getRoundingMode() {
            return RoundingMode.HALF_UP;
        }

        @Override
        public DecimalFormatSymbols getDecimalFormatSymbols() {
            return NLS.getDecimalFormatSymbols();
        }

        @Override
        public String toString() {
            return "TWO_DECIMAL_PLACES";
        }

    };

    /**
     * Describes a format which rounds to integer numbers (no decimal places).
     * <p>
     * It specifies {@link RoundingMode#HALF_UP} as rounding mode and uses
     * the decimal format symbols for the currently active language provided by
     * {@link sirius.kernel.nls.NLS}.
     * </p>
     *
     * @see sirius.kernel.nls.NLS#getDecimalFormatSymbols()
     */
    public static final NumberFormat NO_DECIMAL_PLACES = new NumberFormat() {

        @Override
        public String getSuffix() {
            return null;
        }

        @Override
        public int getScale() {
            return 0;
        }

        @Override
        public RoundingMode getRoundingMode() {
            return RoundingMode.HALF_UP;
        }

        @Override
        public DecimalFormatSymbols getDecimalFormatSymbols() {
            return NLS.getDecimalFormatSymbols();
        }

        @Override
        public String toString() {
            return "NO_DECIMAL_PLACES";
        }
    };

    /**
     * Returns the suffix appended to a formatted string, like a % sign.
     *
     * @return the suffix used by this format
     */
    String getSuffix();

    /**
     * Returns the desired number of decimal places.
     *
     * @return the number of decimal places used by this format
     */
    int getScale();

    /**
     * Determines the rounding mode if more decimal places are available.
     *
     * @return the rounding mode used by this format
     */
    RoundingMode getRoundingMode();

    /**
     * Returns the utilized format symbols when creating a string representation.
     *
     * @return the decimal format symbols used by this format
     */
    DecimalFormatSymbols getDecimalFormatSymbols();
}
