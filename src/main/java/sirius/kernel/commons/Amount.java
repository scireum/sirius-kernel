/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import sirius.kernel.nls.NLS;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Provides a wrapper around <tt>BigDecimal</tt> to perform "exact" computations on numeric values.
 * <p>
 * Adds some extended computations as well as locale aware formatting options to perform "exact" computations on
 * numeric value. The internal representation is <tt>BigDecimal</tt> and uses MathContext.DECIMAL128 for
 * numerical operations. Also the scale of each value is fixed to 5 decimal places after the comma, since this is
 * enough for most business applications and rounds away any rounding errors introduced by doubles.
 * <p>
 * A textual representation can be created by calling one of the <tt>toString</tt> methods or by supplying
 * a {@link NumberFormat}.
 * <p>
 * Being able to be <i>empty</i>, this class handles <tt>null</tt> values gracefully, which simplifies many operations.
 *
 * @see NumberFormat
 * @see BigDecimal
 */
@Immutable
public class Amount implements Comparable<Amount> {

    /**
     * Represents an missing number. This is also the result of division by 0 and other forbidden operations.
     */
    public static final Amount NOTHING = new Amount(null);
    /**
     * Representation of 100.00
     */
    public static final Amount ONE_HUNDRED = new Amount(new BigDecimal(100));
    /**
     * Representation of 0.00
     */
    public static final Amount ZERO = new Amount(BigDecimal.ZERO);
    /**
     * Representation of 1.00
     */
    public static final Amount ONE = new Amount(BigDecimal.ONE);
    /**
     * Representation of 10.00
     */
    public static final Amount TEN = new Amount(BigDecimal.TEN);
    /**
     * Representation of -1.00
     */
    public static final Amount MINUS_ONE = new Amount(new BigDecimal(-1));
    /**
     * Defines the internal precision used for all computations.
     */
    public static final int SCALE = 5;

    private static final String[] METRICS = {"f", "n", "u", "m", "", "K", "M", "G"};
    private static final int NEUTRAL_METRIC = 4;

    private final BigDecimal value;

    private Amount(BigDecimal value) {
        if (value != null) {
            this.value = value.setScale(SCALE, RoundingMode.HALF_UP);
        } else {
            this.value = null;
        }
    }

    /**
     * Converts the given string into a number. If the string is empty, <tt>NOTHING</tt> is returned.
     * If the string is malformed an exception will be thrown.
     *
     * @param value the string value which should be converted into a numeric value.
     * @return an <tt>Amount</tt> representing the given input. <tt>NOTHING</tt> if the input was empty.
     */
    @Nonnull
    public static Amount ofMachineString(@Nullable String value) {
        if (Strings.isEmpty(value)) {
            return NOTHING;
        }
        return of(NLS.parseMachineString(BigDecimal.class, value));
    }

    /**
     * Converts the given string into a number which is formatted according the decimal symbols for the current locale.
     *
     * @param value the string value which should be converted into a numeric value.
     * @return an {@code Amount} representing the given input. {@code NOTHING} if the input was empty.
     * @see NLS
     */
    @Nonnull
    public static Amount ofUserString(@Nullable String value) {
        if (Strings.isEmpty(value)) {
            return NOTHING;
        }
        return NLS.parseUserString(Amount.class, value);
    }

    /**
     * Converts the given value into a number.
     *
     * @param amount the value which should be converted into an <tt>Amount</tt>
     * @return an <tt>Amount</tt> representing the given input. <tt>NOTHING</tt> if the input was empty.
     */
    @Nonnull
    public static Amount of(@Nullable BigDecimal amount) {
        if (amount == null) {
            return NOTHING;
        }
        return new Amount(amount);
    }

    /**
     * Converts the given value into a number.
     *
     * @param amount the value which should be converted into an <tt>Amount</tt>
     * @return an <tt>Amount</tt> representing the given input
     */
    @Nonnull
    public static Amount of(int amount) {
        return of(new BigDecimal(amount));
    }

    /**
     * Converts the given value into a number.
     *
     * @param amount the value which should be converted into an <tt>Amount</tt>
     * @return an <tt>Amount</tt> representing the given input
     */
    @Nonnull
    public static Amount of(long amount) {
        return of(new BigDecimal(amount));
    }

    /**
     * Converts the given value into a number.
     *
     * @param amount the value which should be converted into an <tt>Amount</tt>
     * @return an <tt>Amount</tt> representing the given input
     */
    @Nonnull
    public static Amount of(double amount) {
        return of(BigDecimal.valueOf(amount));
    }

    /**
     * Converts the given value into a number.
     *
     * @param amount the value which should be converted into an <tt>Amount</tt>
     * @return an <tt>Amount</tt> representing the given input. <tt>NOTHING</tt> if the input was empty.
     */
    @Nonnull
    public static Amount of(@Nullable Integer amount) {
        if (amount == null) {
            return NOTHING;
        }
        return of(new BigDecimal(amount));
    }

    /**
     * Converts the given value into a number.
     *
     * @param amount the value which should be converted into an <tt>Amount</tt>
     * @return an <tt>Amount</tt> representing the given input. <tt>NOTHING</tt> if the input was empty.
     */
    @Nonnull
    public static Amount of(@Nullable Long amount) {
        if (amount == null) {
            return NOTHING;
        }
        return of(new BigDecimal(amount));
    }

    /**
     * Converts the given value into a number.
     *
     * @param amount the value which should be converted into an <tt>Amount</tt>
     * @return an <tt>Amount</tt> representing the given input. <tt>NOTHING</tt> if the input was empty.
     */
    @Nonnull
    public static Amount of(@Nullable Double amount) {
        if (amount == null || Double.isInfinite(amount) || Double.isNaN(amount)) {
            return NOTHING;
        }
        return of(new BigDecimal(amount));
    }

    /**
     * Unwraps the internally used <tt>BigDecimal</tt>. May be <tt>null</tt> if this <tt>Amount</tt> is
     * <tt>NOTHING</tt>.
     *
     * @return the internally used <tt>BigDecimal</tt>
     */
    @Nullable
    public BigDecimal getAmount() {
        return value;
    }

    /**
     * Checks if this contains no value.
     *
     * @return <tt>true</tt> if the internal value is null, <tt>false</tt> otherwise
     */
    public boolean isEmpty() {
        return value == null;
    }

    /**
     * Checks if this actual number contains a value or not
     *
     * @return <tt>true</tt> if the internal value is a number, <tt>false</tt> otherwise
     */
    public boolean isFilled() {
        return value != null;
    }

    /**
     * If this actual number if empty, the given value will be returned. Otherwise this will be returned.
     *
     * @param valueIfNothing the value which is used if there is no internal value
     * @return <tt>this</tt> if there is an internal value, <tt>valueIfNothing</tt> otherwise
     */
    @Nonnull
    public Amount fill(@Nonnull Amount valueIfNothing) {
        if (isEmpty()) {
            return valueIfNothing;
        } else {
            return this;
        }
    }

    /**
     * If this actual number if empty, the given supplier is used to compute one. Otherwise this will be returned.
     *
     * @param supplier the supplier which is used to compute a value if there is no internal value
     * @return <tt>this</tt> if there is an internal value, the computed value of <tt>supplier</tt> otherwise
     */
    @Nonnull
    public Amount computeIfNull(Supplier<Amount> supplier) {
        if (isEmpty()) {
            return supplier.get();
        }
        return this;
    }

    /**
     * Increases this number by the given amount in percent. If <tt>increase</tt> is 18 and the value of this is 100,
     * the result would by 118.
     *
     * @param increase the percent value by which the value of this will be increased
     * @return <tt>NOTHING</tt> if this or increase is empty, {@code this * (1 + increase / 100)} otherwise
     */
    @Nonnull
    @CheckReturnValue
    public Amount increasePercent(@Nonnull Amount increase) {
        return times(ONE.add(increase.asDecimal()));
    }

    /**
     * Decreases this number by the given amount in percent. If <tt>decrease</tt> is 10 and the value of this is 100,
     * the result would by 90.
     *
     * @param decrease the percent value by which the value of this will be decreased
     * @return <tt>NOTHING</tt> if this or increase is empty, {@code this * (1 - increase / 100)} otherwise
     */
    @Nonnull
    @CheckReturnValue
    public Amount decreasePercent(@Nonnull Amount decrease) {
        return times(ONE.subtract(decrease.asDecimal()));
    }

    /**
     * Used to multiply two percentages, like two discounts as if they where applied after each other.
     * <p>
     * This can be used to compute the effective discount if two discounts like 15% and 5% are applied after
     * each other. The result would be {@code (15 + 5) - (15 * 5 / 100)} which is <tt>19,25 %</tt>
     *
     * @param percent the second percent value which would be applied after this percent value.
     * @return the effective percent value after both percentages would have been applied
     * or <tt>NOTHING</tt> if one of both was empty.
     */
    @Nonnull
    @CheckReturnValue
    public Amount multiplyPercent(@Nonnull Amount percent) {
        return add(percent).subtract(this.times(percent).divideBy(ONE_HUNDRED));
    }

    /**
     * Adds the given number to <tt>this</tt>, if <tt>other</tt> is not empty. Otherwise <tt>this</tt> will be
     * returned.
     *
     * @param other the operand to add to this.
     * @return an <tt>Amount</tt> representing the sum of <tt>this</tt> and <tt>other</tt> if both values were filled.
     * If <tt>other</tt> is empty, <tt>this</tt> is returned. If this is empty, <tt>NOTHING</tt> is returned.
     */
    @Nonnull
    @CheckReturnValue
    public Amount add(@Nullable Amount other) {
        if (other == null || other.isEmpty()) {
            return this;
        }
        if (isEmpty()) {
            return NOTHING;
        }
        return Amount.of(value.add(other.value));
    }

    /**
     * Subtracts the given number from <tt>this</tt>, if <tt>other</tt> is not empty. Otherwise <tt>this</tt> will be
     * returned.
     *
     * @param other the operand to subtract from this.
     * @return an <tt>Amount</tt> representing the difference of <tt>this</tt> and <tt>other</tt> if both values were
     * filled.
     * If <tt>other</tt> is empty, <tt>this</tt> is returned. If this is empty, <tt>NOTHING</tt> is returned.
     */
    @Nonnull
    @CheckReturnValue
    public Amount subtract(@Nullable Amount other) {
        if (other == null || other.isEmpty()) {
            return this;
        }
        if (isEmpty()) {
            return NOTHING;
        }
        return Amount.of(value.subtract(other.value));
    }

    /**
     * Multiplies the given number with <tt>this</tt>. If either of both is empty, <tt>NOTHING</tt> will be returned.
     *
     * @param other the operand to multiply with this.
     * @return an <tt>Amount</tt> representing the product of <tt>this</tt> and <tt>other</tt> if both values were
     * filled.
     * If <tt>other</tt> is empty or if <tt>this</tt> is empty, <tt>NOTHING</tt> is returned.
     */
    @Nonnull
    @CheckReturnValue
    public Amount times(@Nonnull Amount other) {
        if (other.isEmpty() || isEmpty()) {
            return NOTHING;
        }
        return Amount.of(value.multiply(other.value));
    }

    /**
     * Divides <tt>this</tt> by the given number. If either of both is empty, or the given number is zero,
     * <tt>NOTHING</tt> will be returned. The division uses {@link MathContext#DECIMAL128}
     *
     * @param other the operand to divide this by.
     * @return an <tt>Amount</tt> representing the division of <tt>this</tt> by <tt>other</tt> or <tt>NOTHING</tt>
     * if either of both is empty.
     */
    @Nonnull
    @CheckReturnValue
    public Amount divideBy(@Nullable Amount other) {
        if (other == null || other.isZeroOrNull() || isEmpty()) {
            return NOTHING;
        }
        return Amount.of(value.divide(other.value, MathContext.DECIMAL128));
    }

    /**
     * Returns the ratio in percent from <tt>this</tt> to <tt>other</tt>.
     * This is equivalent to {@code this / other * 100}
     *
     * @param other the base to compute the percentage from.
     * @return an <tt>Amount</tt> representing the ratio between <tt>this</tt> and <tt>other</tt>
     * or <tt>NOTHING</tt> if either of both is empty.
     */
    @Nonnull
    @CheckReturnValue
    public Amount percentageOf(@Nullable Amount other) {
        return divideBy(other).toPercent();
    }

    /**
     * Returns the increase in percent of <tt>this</tt> over <tt>other</tt>.
     * This is equivalent to {@code ((this / other) - 1) * 100}
     *
     * @param other the base to compute the increase from.
     * @return an <tt>Amount</tt> representing the percentage increase between <tt>this</tt> and <tt>other</tt>
     * or <tt>NOTHING</tt> if either of both is empty.
     */
    @Nonnull
    @CheckReturnValue
    public Amount percentageDifferenceOf(@Nonnull Amount other) {
        return divideBy(other).subtract(ONE).toPercent();
    }

    /**
     * Determines if the value is filled and equal to 0.00.
     *
     * @return <tt>true</tt> if this value is filled and equal to 0.00, <tt>false</tt> otherwise.
     */
    public boolean isZero() {
        return value != null && value.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Determines if the value is filled and not equal to 0.00.
     *
     * @return <tt>true</tt> if this value is filled and not equal to 0.00, <tt>false</tt> otherwise.
     */
    public boolean isNonZero() {
        return value != null && value.compareTo(BigDecimal.ZERO) != 0;
    }

    /**
     * Determines if the value is filled and greater than 0.00
     *
     * @return <tt>true</tt> if this value is filled and greater than 0.00, <tt>false</tt> otherwise.
     */
    public boolean isPositive() {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Determines if the value is filled and less than 0.00
     *
     * @return <tt>true</tt> if this value is filled and less than 0.00, <tt>false</tt> otherwise.
     */
    public boolean isNegative() {
        return value != null && value.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Determines if the value is empty or equal to 0.00
     *
     * @return <tt>true</tt> if this value is empty, or equal to 0.00, <tt>false</tt> otherwise.
     */
    public boolean isZeroOrNull() {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Converts a given decimal fraction into a percent value i.e. 0.34 to 34 %.
     * Effectively this is {@code this * 100}
     *
     * @return a percentage representation of the given decimal value.
     */
    @Nonnull
    @CheckReturnValue
    public Amount toPercent() {
        return this.times(ONE_HUNDRED);
    }

    /**
     * Converts a percent value into a decimal fraction i.e. 34 % to 0.34
     * Effectively this is {@code this / 100}
     *
     * @return a decimal representation fo the given percent value.
     */
    @Nonnull
    @CheckReturnValue
    public Amount asDecimal() {
        return divideBy(ONE_HUNDRED);
    }

    /**
     * Compares this amount against the given one.
     * <p>
     * If both amounts are empty, they are considered equal, otherwise, if the given amount is empty, we consider this
     * amount to be larger. Likewise, if this amount is empty, we consider the given one to be larger.
     *
     * @param o the amount to compare to
     * @return 0 if both amounts are equal, -1 of this amount is less than the given one or 1 if this amount is greater
     * than the given one
     */
    @Override
    @SuppressWarnings("squid:S1698")
    @Explain("Indentity against this is safe and a shortcut to speed up comparisons")
    public int compareTo(Amount o) {
        if (o == null) {
            return 1;
        }
        if (o == this) {
            return 0;
        }
        if (Objects.equals(value, o.value)) {
            return 0;
        }
        if (o.value == null) {
            return 1;
        }
        if (value == null) {
            return -1;
        }
        return value.compareTo(o.value);
    }

    /**
     * Determines if this amount is greater than the given one.
     * <p>
     * See {@link #compareTo(Amount)} for an explanation of how empty amounts are handled.
     *
     * @param o the other amount to compare against
     * @return <tt>true</tt> if this amount is greater than the given one
     */
    public boolean isGreaterThan(Amount o) {
        return compareTo(o) > 0;
    }

    /**
     * Determines if this amount is greater than or equal to the given one.
     * <p>
     * See {@link #compareTo(Amount)} for an explanation of how empty amounts are handled.
     *
     * @param o the other amount to compare against
     * @return <tt>true</tt> if this amount is greater than or equals to the given one
     */
    public boolean isGreaterThanOrEqual(Amount o) {
        return compareTo(o) >= 0;
    }

    /**
     * Determines if this amount is less than the given one.
     * <p>
     * See {@link #compareTo(Amount)} for an explanation of how empty amounts are handled.
     *
     * @param o the other amount to compare against
     * @return <tt>true</tt> if this amount is less than the given one
     */
    public boolean isLessThan(Amount o) {
        return compareTo(o) < 0;
    }

    /**
     * Determines if this amount is less than or equal to the given one.
     * <p>
     * See {@link #compareTo(Amount)} for an explanation of how empty amounts are handled.
     *
     * @param o the other amount to compare against
     * @return <tt>true</tt> if this amount is less than or equals to the given one
     */
    public boolean isLessThanOrEqual(Amount o) {
        return compareTo(o) <= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Amount otherAmount = (Amount) o;
        if (this.value == null || otherAmount.value == null) {
            return (this.value == null) == (otherAmount.value == null);
        }

        return this.value.compareTo(otherAmount.value) == 0;
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }

    /**
     * Compares this amount against the given amount and returns the one with the lower value.
     * <p>
     * If either of the amounts is empty, the filled one is returned. If both are empty, an empty amount is returned.
     *
     * @param other the amount to compare against
     * @return the amount with the lower value or an empty amount, if both amounts are empty
     */
    @Nonnull
    @SuppressWarnings("squid:S1698")
    @Explain("Indentity against this is safe and a shortcut to speed up comparisons")
    public Amount min(@Nullable Amount other) {
        if (other == this || other == null) {
            return this;
        }
        if (isEmpty()) {
            return other;
        }
        if (other.isEmpty()) {
            return this;
        }

        return this.value.compareTo(other.value) < 0 ? this : other;
    }

    /**
     * Compares this amount against the given amount and returns the one with the higher value.
     * <p>
     * If either of the amounts is empty, the filled one is returned. If both are empty, an empty amount is returned.
     *
     * @param other the amount to compare against
     * @return the amount with the higher value or an empty amount, if both amounts are empty
     */
    @Nonnull
    @SuppressWarnings("squid:S1698")
    @Explain("Indentity against this is safe and a shortcut to speed up comparisons")
    public Amount max(@Nullable Amount other) {
        if (other == this || other == null) {
            return this;
        }
        if (isEmpty()) {
            return other;
        }
        if (other.isEmpty()) {
            return this;
        }

        return this.value.compareTo(other.value) > 0 ? this : other;
    }

    /**
     * Negates <tt>this</tt> amount and returns the new amount.
     *
     * @return an <tt>Amount</tt> representing the negated <tt>Amount</tt>. If <tt>this</tt> is empty, <tt>NOTHING</tt>
     * is returned.
     */
    public Amount negate() {
        return times(MINUS_ONE);
    }

    @Override
    public String toString() {
        return toSmartRoundedString(NumberFormat.TWO_DECIMAL_PLACES).toString();
    }

    /**
     * Formats the represented value as percentage. Up to two digits will be shown if non zero.
     * Therefore <tt>12.34</tt> will be rendered as <tt>12.34 %</tt> but <tt>5.00</tt> will be
     * rendered as <tt>5 %</tt>. If the value is empty, "" will be returned.
     * <p>
     * A custom {@link NumberFormat} can be used by directly calling {@link #toSmartRoundedString(NumberFormat)}
     * or to disable smart rounding (to also show .00) {@link #toString(NumberFormat)} can be called using
     * {@link NumberFormat#PERCENT}.
     *
     * @return a string representation of this number using {@code NumberFormat#PERCENT}
     * or "" if the value is empty.
     */
    public String toPercentString() {
        return toSmartRoundedString(NumberFormat.PERCENT).toString();
    }

    /**
     * Tries to convert the wrapped value to a roman numeral representation.
     * Any fractional part of this {@code BigDecimal} will be discarded,
     * and if the resulting "{@code BigInteger}" is too big to fit in an {@code int}, only the low-order 32 bits are
     * returned.
     *
     * @return a string representation of this number as roman numeral or "" for values &lt;= 0 and &gt;= 4000.
     */
    public String toRomanNumeralString() {
        return RomanNumeral.toRoman(value.intValue());
    }

    /**
     * Formats the represented value by rounding to zero decimal places. The rounding mode is obtained from
     * {@link NumberFormat#NO_DECIMAL_PLACES}.
     *
     * @return a rounded representation of this number using {@code NumberFormat#NO_DECIMAL_PLACES}
     * or "" is the value is empty.
     */
    public String toRoundedString() {
        return toSmartRoundedString(NumberFormat.NO_DECIMAL_PLACES).toString();
    }

    /**
     * Rounds the number according to the given format. In contrast to only round values when displaying them as
     * string, this method returns a modified <tt>Amount</tt> which as potentially lost some precision. Depending on
     * the next computation this might return significantly different values in contrast to first performing all
     * computations and round at the end when rendering the values as string.
     * <p>
     * The number of decimal places and the rounding mode is obtained from <tt>format</tt> ({@link NumberFormat}).
     *
     * @param format the format used to determine the precision of the rounding operation
     * @return returns an <tt>Amount</tt> which is rounded using the given {@code NumberFormat}
     * or <tt>NOTHING</tt> if the value is empty.
     */
    @Nonnull
    @CheckReturnValue
    public Amount round(@Nonnull NumberFormat format) {
        return round(format.getScale(), format.getRoundingMode());
    }

    /**
     * Rounds the number according to the given format. In contrast to only round values when displaying them as
     * string, this method returns a modified <tt>Amount</tt> which as potentially lost some precision. Depending on
     * the next computation this might return significantly different values in contrast to first performing all
     * computations and round at the end when rendering the values as string.
     *
     * @param scale        the precision
     * @param roundingMode the rounding operation
     * @return returns an <tt>Amount</tt> which is rounded using the given {@code RoundingMode}
     * or <tt>NOTHING</tt> if the value is empty.
     */
    @Nonnull
    @CheckReturnValue
    public Amount round(int scale, @Nonnull RoundingMode roundingMode) {
        if (isEmpty()) {
            return NOTHING;
        }

        return Amount.of(value.setScale(scale, roundingMode));
    }

    private Value convertToString(NumberFormat format, boolean smartRound) {
        if (isEmpty()) {
            return Value.of(null);
        }

        DecimalFormat df = new DecimalFormat();
        df.setMinimumFractionDigits(smartRound ? 0 : format.getScale());
        df.setMaximumFractionDigits(format.getScale());
        df.setDecimalFormatSymbols(format.getDecimalFormatSymbols());
        df.setGroupingUsed(format.isUseGrouping());

        return Value.of(df.format(value)).append(" ", format.getSuffix());
    }

    /**
     * Converts the number into a string according to the given <tt>format</tt>. The returned {@link Value} provides
     * helpful methods to pre- or append texts like units or currency symbols while gracefully handling empty values.
     *
     * @param format the {@code NumberFormat} used to obtain the number of decimal places,
     *               the decimal format symbols and rounding mode
     * @return a <tt>Value</tt> containing the string representation according to the given format
     * or an empty <tt>Value</tt> if <tt>this</tt> is empty.
     * @see Value#append(String, Object)
     * @see Value#prepend(String, Object)
     */
    @Nonnull
    public Value toString(@Nonnull NumberFormat format) {
        return convertToString(format, false);
    }

    /**
     * Converts the number into a string just like {@link #toString(NumberFormat)}. However, if the number has no
     * decimal places, a rounded value (without .00) will be returned.
     *
     * @param format the {@code NumberFormat} used to obtain the number of decimal places,
     *               the decimal format symbols and rounding mode
     * @return a <tt>Value</tt> containing the string representation according to the given format
     * or an empty <tt>Value</tt> if <tt>this</tt> is empty. Omits 0 as decimal places.
     * @see #toString()
     */
    @Nonnull
    public Value toSmartRoundedString(@Nonnull NumberFormat format) {
        return convertToString(format, true);
    }

    /**
     * Creates a "scientific" representation of the amount.
     * <p>
     * This representation will shift the value in the range 0..999 and represent the decimal shift by a well
     * known unit prefix. The following prefixes will be used:
     * <ul>
     * <li>f - femto</li>
     * <li>n - nano</li>
     * <li>u - micro</li>
     * <li>m - milli</li>
     * <li>K - kilo</li>
     * <li>M - mega</li>
     * <li>G - giga</li>
     * </ul>
     * <p>
     * An input of <tt>0.0341 V</tt> will be represented as <tt>34.1 mV</tt> if digits was 4 or 34 mV is digits was 2
     * or less.
     *
     * @param digits the number of decimal digits to display
     * @param unit   the unit to be appended to the generated string
     * @return a scientific string representation of the amount.
     */
    @Nonnull
    public String toScientificString(int digits, String unit) {
        if (isEmpty()) {
            return "";
        }
        int metric = NEUTRAL_METRIC;
        double doubleValue = this.value.doubleValue();
        while (Math.abs(doubleValue) >= 990d && metric < METRICS.length - 1) {
            doubleValue /= 1000d;
            metric += 1;
        }
        int effectiveDigits = digits;
        if (metric == NEUTRAL_METRIC) {
            while (!Doubles.isZero(doubleValue) && Math.abs(doubleValue) < 1.01 && metric > 0) {
                doubleValue *= 1000d;
                metric -= 1;
                // We loose accuracy, therefore we limit our decimal digits...
                effectiveDigits -= 3;
            }
        }
        DecimalFormat df = new DecimalFormat();
        df.setMinimumFractionDigits(Math.max(0, effectiveDigits));
        df.setMaximumFractionDigits(Math.max(0, effectiveDigits));
        df.setDecimalFormatSymbols(NLS.getDecimalFormatSymbols());
        df.setGroupingUsed(true);
        StringBuilder sb = new StringBuilder(df.format(doubleValue));
        if (metric != NEUTRAL_METRIC) {
            sb.append(" ");
            sb.append(METRICS[metric]);
        }
        if (unit != null) {
            sb.append(unit);
        }

        return sb.toString();
    }

    /**
     * Returns the number of decimal digits (ignoring decimal places after the decimal separator).
     *
     * @return the number of digits required to represent this number. Returns 0 if the value is empty.
     */
    public long getDigits() {
        if (value == null) {
            return 0;
        }
        return Math.round(Math.floor(Math.log10(value.doubleValue()) + 1));
    }
}
