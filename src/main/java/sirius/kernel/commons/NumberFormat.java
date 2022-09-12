/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import sirius.kernel.nls.NLS;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.RoundingMode;
import java.text.DecimalFormatSymbols;

/**
 * Used to define the parameters used to create a string representation of a number.
 * <p>
 * Provides a set of default formats and also describes the parameters used to format a number.
 * This is used by {@link Amount} to create string representations.
 */
public class NumberFormat {

    /**
     * Describes the default format used to create string representations of percentages.
     * <p>
     * It therefore specifies two decimal places which are rounded {@link RoundingMode#HALF_UP}.
     * It uses the decimal format symbols for the currently active language provided by
     * {@link sirius.kernel.nls.NLS}. A percent sign <tt>%</tt> is used as suffix
     *
     * @see sirius.kernel.nls.NLS#getDecimalFormatSymbols()
     */
    public static final NumberFormat PERCENT = new NumberFormat(2, RoundingMode.HALF_UP, null, true, "%");

    /**
     * Describes a format which rounds to two decimal places.
     * <p>
     * It specifies {@link RoundingMode#HALF_UP} as rounding mode and uses
     * the decimal format symbols for the currently active language provided by
     * {@link sirius.kernel.nls.NLS}.
     *
     * @see sirius.kernel.nls.NLS#getDecimalFormatSymbols()
     */
    public static final NumberFormat TWO_DECIMAL_PLACES = new NumberFormat(2, RoundingMode.HALF_UP, null, true, null);

    /**
     * Describes a format which rounds to two decimal places.
     * <p>
     * It specifies {@link RoundingMode#HALF_UP} as rounding mode and uses
     * the decimal format symbols for machine formats, provided by
     * {@link sirius.kernel.nls.NLS}.
     *
     * @see sirius.kernel.nls.NLS#getMachineFormatSymbols()
     */
    public static final NumberFormat MACHINE_TWO_DECIMAL_PLACES =
            new NumberFormat(2, RoundingMode.HALF_UP, NLS.getMachineFormatSymbols(), false, null);

    /**
     * Describes a format which rounds to three decimal places.
     * <p>
     * It specifies {@link RoundingMode#HALF_UP} as rounding mode and uses
     * the decimal format symbols for machine formats, provided by
     * {@link sirius.kernel.nls.NLS}.
     *
     * @see sirius.kernel.nls.NLS#getMachineFormatSymbols()
     */
    public static final NumberFormat MACHINE_THREE_DECIMAL_PLACES =
            new NumberFormat(3, RoundingMode.HALF_UP, NLS.getMachineFormatSymbols(), false, null);

    /**
     * Describes a format which rounds to up to five decimal places.
     * <p>
     * It specifies {@link RoundingMode#HALF_UP} as rounding mode and uses
     * the decimal format symbols for machine formats, provided by
     * {@link sirius.kernel.nls.NLS}.
     * <p>
     * This method is intended to return a machine representation without loosing any data while formatting. As most
     * probably five decimal places are too much to output, this should be used in conjuction with
     * {@link Amount#toSmartRoundedString(NumberFormat)}.
     *
     * @see sirius.kernel.nls.NLS#getMachineFormatSymbols()
     */
    public static final NumberFormat MACHINE_FIVE_DECIMAL_PLACES =
            new NumberFormat(5, RoundingMode.HALF_UP, NLS.getMachineFormatSymbols(), false, null);

    /**
     * Describes a format which rounds to integer numbers (no decimal places).
     * <p>
     * It specifies {@link RoundingMode#HALF_UP} as rounding mode and uses
     * the decimal format symbols for the currently active language provided by
     * {@link sirius.kernel.nls.NLS}.
     *
     * @see sirius.kernel.nls.NLS#getDecimalFormatSymbols()
     */
    public static final NumberFormat NO_DECIMAL_PLACES = new NumberFormat(0, RoundingMode.HALF_UP, null, true, null);

    /**
     * Describes a format which rounds to integer numbers (no decimal places).
     * <p>
     * It specifies {@link RoundingMode#HALF_UP} as rounding mode and uses
     * the decimal format symbols for machine formats, provided by
     * {@link sirius.kernel.nls.NLS}.
     *
     * @see sirius.kernel.nls.NLS#getMachineFormatSymbols()
     */
    public static final NumberFormat MACHINE_NO_DECIMAL_PLACES =
            new NumberFormat(0, RoundingMode.HALF_UP, NLS.getMachineFormatSymbols(), false, null);

    private final String suffix;
    private final int scale;
    private final boolean useGrouping;
    private final RoundingMode roundingMode;
    private final DecimalFormatSymbols formatSymbols;

    /**
     * Creates a new number format used to format {@link Amount amounts}.
     *
     * @param scale         contains the number of decimal places shown. Use {@link Amount#toSmartRoundedString(NumberFormat)}
     *                      to remove unwanted zeros.
     * @param roundingMode  contains the rounding mode to use. Most commonly {@link RoundingMode#HALF_UP} will be
     *                      correct.
     * @param formatSymbols contains the {@link DecimalFormatSymbols} to use. This parameter can be <tt>null</tt> to
     *                      use the format symbols of the current language, which is present, when this format is used.
     * @param useGrouping   determines if grouping (by thousands) is used.
     * @param suffix        the suffix to append to a formatted string
     */
    public NumberFormat(int scale,
                        @Nonnull RoundingMode roundingMode,
                        @Nullable DecimalFormatSymbols formatSymbols,
                        boolean useGrouping,
                        @Nullable String suffix) {
        this.scale = scale;
        this.roundingMode = roundingMode;
        this.formatSymbols = formatSymbols;
        this.useGrouping = useGrouping;
        this.suffix = suffix;
    }

    /**
     * Returns the suffix appended to a formatted string, like a % sign.
     *
     * @return the suffix used by this format
     */
    public String getSuffix() {
        return suffix;
    }

    /**
     * Returns the desired number of decimal places.
     *
     * @return the number of decimal places used by this format
     */
    public int getScale() {
        return scale;
    }

    /**
     * Determines the rounding mode if more decimal places are available.
     *
     * @return the rounding mode used by this format
     */
    public RoundingMode getRoundingMode() {
        return roundingMode;
    }

    /**
     * Returns the utilized format symbols when creating a string representation.
     *
     * @return the decimal format symbols used by this format
     */
    public DecimalFormatSymbols getDecimalFormatSymbols() {
        if (formatSymbols == null) {
            return NLS.getDecimalFormatSymbols();
        }

        return formatSymbols;
    }

    /**
     * Determines if grouping separaters (by thousands) should be placed or not.
     *
     * @return <tt>true</tt> if grouping separators are used, <tt>false</tt> otherwise
     */
    public boolean isUseGrouping() {
        return useGrouping;
    }
}
