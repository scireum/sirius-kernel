/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import sirius.kernel.SiriusExtension
import sirius.kernel.async.CallContext
import java.math.RoundingMode
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * Tests the [Amount] class.
 */
@ExtendWith(SiriusExtension::class)
class AmountTest {

    companion object {
        @JvmStatic
        private fun `generator for add() works as expected`(): Stream<Arguments?>? {
            return Stream.of(
                    Arguments.of(
                            4.2, 42, 46.2
                    ),
                    Arguments.of(
                            42, 4.2, 46.2
                    ),
                    Arguments.of(
                            Amount.ZERO, 42, 42
                    ),
                    Arguments.of(
                            42, Amount.ZERO, 42
                    ),
                    Arguments.of(
                            Amount.NOTHING, 42, Amount.NOTHING
                    ),
                    Arguments.of(
                            42, Amount.NOTHING, 42
                    ),
            )
        }

        @JvmStatic
        private fun `generator for subtract() works as expected`(): Stream<Arguments?>? {
            return Stream.of(
                    Arguments.of(
                            4.2, 42, -37.8
                    ),
                    Arguments.of(
                            42, 4.2, 37.8
                    ),
                    Arguments.of(
                            Amount.ZERO, 42, -42
                    ),
                    Arguments.of(
                            42, Amount.ZERO, 42
                    ),
                    Arguments.of(
                            Amount.NOTHING, 42, Amount.NOTHING
                    ),
                    Arguments.of(
                            42, Amount.NOTHING, 42
                    ),
            )
        }

        @JvmStatic
        private fun `generator for times() works as expected`(): Stream<Arguments?>? {
            return Stream.of(
                    Arguments.of(
                            4.2, 42, 176.4
                    ),
                    Arguments.of(
                            42, 4.2, 176.4
                    ),
                    Arguments.of(Amount.ZERO, 42, Amount.ZERO),
                    Arguments.of(
                            42, Amount.ZERO, Amount.ZERO
                    ),
                    Arguments.of(
                            Amount.NOTHING, 42, Amount.NOTHING
                    ),
                    Arguments.of(
                            42, Amount.NOTHING, Amount.NOTHING
                    ),
            )
        }

        @JvmStatic
        private fun `generator for divideBy() works as expected`(): Stream<Arguments?>? {
            return Stream.of(
                    Arguments.of(
                            4.2, 42, 0.1
                    ),
                    Arguments.of(
                            42, 4.2, Amount.TEN
                    ),
                    Arguments.of(
                            Amount.ZERO, 42, Amount.ZERO
                    ),
                    Arguments.of(
                            42, Amount.ZERO, Amount.NOTHING
                    ),
                    Arguments.of(
                            Amount.NOTHING, 42, Amount.NOTHING
                    ),
                    Arguments.of(
                            42, Amount.NOTHING, Amount.NOTHING
                    ),
            )
        }

        @JvmStatic
        private fun `generator for negate() works as expected`(): Stream<Arguments?>? {
            return Stream.of(
                    Arguments.of(
                            4.2, -4.2
                    ),
                    Arguments.of(
                            -4.2, 4.2
                    ),
                    Arguments.of(
                            42, -42
                    ),
                    Arguments.of(
                            Amount.ZERO, Amount.ZERO
                    ),
                    Arguments.of(
                            -0, Amount.ZERO
                    ),
                    Arguments.of(
                            Amount.NOTHING, Amount.NOTHING
                    )
            )
        }

        @JvmStatic
        private fun `generator for increasePercent() works as expected`(): Stream<Arguments?>? {
            return Stream.of(
                    Arguments.of(
                            4.2, Amount.TEN, 4.62
                    ),
                    Arguments.of(
                            Amount.ZERO, 42, Amount.ZERO
                    ),
                    Arguments.of(
                            42, Amount.ZERO, 42
                    ),
                    Arguments.of(
                            Amount.NOTHING, 42, Amount.NOTHING
                    ),
                    Arguments.of(
                            42, Amount.NOTHING, 42
                    ),
            )
        }

        @JvmStatic
        private fun `generator for decreasePercent() works as expected`(): Stream<Arguments?>? {
            return Stream.of(
                    Arguments.of(
                            4.2, Amount.TEN, 3.78
                    ),
                    Arguments.of(
                            Amount.ZERO, 42, Amount.ZERO
                    ),
                    Arguments.of(
                            42, Amount.ZERO, 42
                    ),
                    Arguments.of(
                            Amount.NOTHING, 42, Amount.NOTHING
                    ),
                    Arguments.of(
                            42, Amount.NOTHING, 42
                    ),
            )
        }

        @JvmStatic
        private fun `generator for percentageOf() works as expected`(): Stream<Arguments?>? {
            return Stream.of(
                    Arguments.of(
                            4.2, 42, Amount.TEN
                    ),
                    Arguments.of(
                            Amount.ZERO, 42, Amount.ZERO
                    ),
                    Arguments.of(
                            42, Amount.ZERO, Amount.NOTHING
                    ),
                    Arguments.of(
                            Amount.NOTHING, 42, Amount.NOTHING
                    ),
                    Arguments.of(
                            42, Amount.NOTHING, Amount.NOTHING
                    ),
            )
        }

        @JvmStatic
        private fun `generator for percentageDifferenceOf() works as expected`(): Stream<Arguments?>? {
            return Stream.of(
                    Arguments.of(
                            4.62, 4.2, Amount.TEN
                    ),
                    Arguments.of(
                            Amount.ZERO, 42, -100
                    ),
                    Arguments.of(
                            42, Amount.ZERO, Amount.NOTHING
                    ),
                    Arguments.of(
                            Amount.NOTHING, 42, Amount.NOTHING
                    ),
                    Arguments.of(
                            42, Amount.NOTHING, Amount.NOTHING
                    ),
            )
        }

        @JvmStatic
        private fun `generator for toPercent() works as expected`(): Stream<Arguments?>? {
            return Stream.of(
                    Arguments.of(
                            0.42, 42
                    ),
                    Arguments.of(
                            Amount.ONE, Amount.ONE_HUNDRED
                    ),
                    Arguments.of(
                            2, 200
                    ),
                    Arguments.of(
                            Amount.ZERO, Amount.ZERO
                    ),
                    Arguments.of(
                            Amount.NOTHING, Amount.NOTHING
                    ),
            )
        }

        @JvmStatic
        private fun `generator for asDecimal() works as expected`(): Stream<Arguments?>? {
            return Stream.of(
                    Arguments.of(
                            42, 0.42
                    ),
                    Arguments.of(
                            Amount.ONE_HUNDRED, Amount.ONE
                    ),
                    Arguments.of(
                            200, 2
                    ),
                    Arguments.of(
                            Amount.ZERO, Amount.ZERO
                    ),
                    Arguments.of(
                            Amount.NOTHING, Amount.NOTHING
                    ),
            )
        }

        @JvmStatic
        private fun `generator for remainder() works as expected`(): Stream<Arguments?>? {
            return Stream.of(
                    Arguments.of(
                            10, 2, 0
                    ),
                    Arguments.of(
                            10, 3, 1
                    ),
                    Arguments.of(
                            10, 0, Amount.NOTHING
                    ),
                    Arguments.of(
                            0, 10, 0
                    ),
                    Arguments.of(
                            Amount.NOTHING, 10, Amount.NOTHING
                    ),
                    Arguments.of(
                            10, Amount.NOTHING, Amount.NOTHING
                    )
            )
        }
    }


    @Test
    fun `predicates are evaluated correctly`() {
        assertTrue { Amount.NOTHING.isEmpty }
        assertTrue { Amount.NOTHING.isZeroOrNull }
        assertTrue { Amount.ZERO.isZeroOrNull }
        assertTrue { Amount.ZERO.isZero }
        assertTrue { Amount.MINUS_ONE.isNonZero }
        assertTrue { Amount.MINUS_ONE.isNegative }
        assertTrue { Amount.TEN.isPositive }
        assertTrue { Amount.TEN > Amount.ONE }
    }

    @Test
    fun `Amountof converts various types correctly`() {
        CallContext.getCurrent().setLanguage("en")
        assertEquals(Amount.ONE, Amount.of(1.0))
        assertEquals(Amount.ONE, Amount.of(1L))
        assertEquals(Amount.ONE, Amount.of(Integer.valueOf(1)))
        assertEquals(Amount.ONE, Amount.of((1L).toLong()))
        assertEquals(Amount.ONE, Amount.of((1.0).toDouble()))
        assertEquals(Amount.ONE, Amount.ofMachineString("1.0"))
        assertEquals(Amount.ONE, Amount.ofUserString("1.0"))
    }

    @Test
    fun `ofMachineString works correctly`() {
        assertEquals(Amount.ONE, Amount.ofMachineString("1"))
        assertEquals(Amount.ONE.divideBy(Amount.TEN), Amount.ofMachineString("0.1"))
        assertEquals(Amount.TEN, Amount.ofMachineString("10"))
    }

    @Test
    fun `Computations with NOTHING result in expected values`() {
        assertEquals(Amount.ONE, Amount.ONE.add(Amount.NOTHING))
        assertEquals(Amount.NOTHING, Amount.NOTHING.add(Amount.ONE))
        assertEquals(Amount.NOTHING, Amount.NOTHING.add(Amount.NOTHING))
        assertEquals(Amount.of((2).toInt()), Amount.ONE.add(Amount.ONE))
        assertEquals(Amount.MINUS_ONE, Amount.ONE.subtract(Amount.of((2).toInt())))
        assertEquals(Amount.ONE_HUNDRED, Amount.TEN.times(Amount.TEN))
        assertEquals(Amount.NOTHING, Amount.TEN.times(Amount.NOTHING))
        assertEquals(Amount.NOTHING, Amount.NOTHING.times(Amount.TEN))
        assertEquals(Amount.NOTHING, Amount.ONE.divideBy(Amount.ZERO))
        assertEquals(Amount.NOTHING, Amount.ONE.divideBy(Amount.NOTHING))
        assertEquals(Amount.NOTHING, Amount.NOTHING.divideBy(Amount.ONE))
        assertEquals(Amount.TEN, Amount.ONE_HUNDRED.divideBy(Amount.TEN))
        assertEquals(Amount.of((2).toInt()), Amount.ONE.increasePercent(Amount.ONE_HUNDRED))
        assertEquals(Amount.of((0.5).toDouble()), Amount.ONE.decreasePercent(Amount.of((50).toInt())))
    }

    @Test
    fun `fill and orElseGet are only evaluated if no value is present`() {
        assertEquals(Amount.TEN, Amount.NOTHING.fill(Amount.TEN))
        assertEquals(Amount.TEN, Amount.TEN.fill(Amount.ONE))
        assertEquals(Amount.NOTHING.orElseGet { Amount.TEN }, Amount.TEN)
    }

    @Test
    fun `helper functions compute correct values`() {
        assertEquals(Amount.TEN, Amount.TEN.percentageOf(Amount.ONE_HUNDRED))
        assertEquals(Amount.ONE_HUNDRED, Amount.TEN.percentageDifferenceOf(Amount.of((5).toInt())))
        assertEquals(Amount.of((-50).toInt()), Amount.of((5).toInt()).percentageDifferenceOf(Amount.TEN))
        assertEquals(Amount.of((0.5).toDouble()), Amount.of((50).toInt()).asDecimal())
        assertEquals(3, Amount.ONE_HUNDRED.digits)
        assertEquals(1, Amount.ONE.getDigits())
        assertEquals(2, Amount.ONE_HUNDRED.subtract(Amount.ONE).getDigits())
        assertEquals(3, Amount.of((477).toInt()).getDigits())
    }

    @Test
    fun `rounding works as expected`() {
        assertEquals("1.23", Amount.ofMachineString("1.23223").round(2, RoundingMode.HALF_UP).toMachineString())
        assertEquals("1.232", Amount.ofMachineString("1.23223").round(3, RoundingMode.HALF_UP).toMachineString())
        assertEquals("1.2", Amount.ofMachineString("1.23223").round(1, RoundingMode.HALF_UP).toMachineString())
    }

    @Test
    fun `formatting works as expected`() {
        CallContext.getCurrent().setLanguage("en")
        assertEquals("10 %", Amount.ofMachineString("0.1").toPercent().toPercentString())
        assertEquals("1", Amount.of(1.23).toRoundedString())
        assertEquals("1", Amount.of(1.00).toSmartRoundedString(NumberFormat.TWO_DECIMAL_PLACES).asString())
        assertEquals("1.00", Amount.of(1.00).toString(NumberFormat.TWO_DECIMAL_PLACES).asString())
        assertEquals("1.23", Amount.of(1.23).toSmartRoundedString(NumberFormat.TWO_DECIMAL_PLACES).asString())
        assertEquals("12 m", Amount.of(0.012).toScientificString(0, ""))
        assertEquals("1.2 K", Amount.of((1200).toInt()).toScientificString(1, ""))
    }

    @Test
    fun `min selects the lower value and handles null and NOTHING gracefully`() {
        assertEquals(Amount.ONE, Amount.ONE.min(Amount.TEN))
        assertEquals(Amount.ONE, Amount.TEN.min(Amount.ONE))
        assertEquals(Amount.ONE, Amount.NOTHING.min(Amount.ONE))
        assertEquals(Amount.ONE, Amount.ONE.min(Amount.NOTHING))
        assertEquals(Amount.NOTHING, Amount.NOTHING.min(Amount.NOTHING))
        assertEquals(Amount.TEN, Amount.TEN.min(null))
        assertEquals(Amount.ZERO, Amount.ZERO.min(Amount.NOTHING))
    }

    @Test
    fun `max selects the higher value and handles null and NOTHING gracefully`() {
        assertEquals(Amount.TEN, Amount.ONE.max(Amount.TEN))
        assertEquals(Amount.TEN, Amount.TEN.max(Amount.ONE))
        assertEquals(Amount.ONE, Amount.NOTHING.max(Amount.ONE))
        assertEquals(Amount.ONE, Amount.ONE.max(Amount.NOTHING))
        assertEquals(Amount.NOTHING, Amount.NOTHING.max(Amount.NOTHING))
        assertEquals(Amount.TEN, Amount.TEN.max(null))
        assertEquals(Amount.ZERO, Amount.ZERO.max(Amount.NOTHING))
    }

    @Test
    fun `compare returns which amount is higher`() {
        assertEquals(0, Amount.NOTHING.compareTo(Amount.NOTHING))
        assertTrue { Amount.ONE.compareTo(Amount.NOTHING) > 0 }
        assertTrue { Amount.NOTHING.compareTo(Amount.ONE) < 0 }
        assertEquals(0, Amount.ONE.compareTo(Amount.ONE))
        assertTrue { Amount.ONE.compareTo(Amount.MINUS_ONE) > 0 }
        assertTrue { Amount.MINUS_ONE.compareTo(Amount.ONE) < 0 }
    }

    @Test
    fun `boilerplace comparators work`() {
        assertTrue { Amount.ONE.isGreaterThan(Amount.NOTHING) }
        assertTrue { Amount.ONE.isGreaterThanOrEqual(Amount.NOTHING) }
        assertTrue { Amount.NOTHING.isLessThan(Amount.ONE) }
        assertTrue { Amount.NOTHING.isLessThanOrEqual(Amount.ONE) }
        assertTrue { Amount.ONE.isGreaterThanOrEqual(Amount.ONE) }
        assertTrue { Amount.ONE.isLessThanOrEqual(Amount.ONE) }
        assertTrue { Amount.ONE.isGreaterThan(Amount.MINUS_ONE) }
        assertTrue { Amount.MINUS_ONE.isLessThan(Amount.ONE) }
    }

    @ParameterizedTest
    @MethodSource("generator for add() works as expected")
    fun `add() works as expected`(
            a: Number,
            b: Number,
            result: Number,
    ) {
        val amountA = if (a is Amount) a else Amount.of(a.toDouble())
        val amountB = if (b is Amount) b else Amount.of(b.toDouble())
        val amountResult = if (result is Amount) result else Amount.of(result.toDouble())
        assertEquals(amountResult, amountA.add(amountB))
    }

    @ParameterizedTest
    @MethodSource("generator for subtract() works as expected")
    fun `subtract() works as expected`(
            a: Number,
            b: Number,
            result: Number,
    ) {
        val amountA = if (a is Amount) a else Amount.of(a.toDouble())
        val amountB = if (b is Amount) b else Amount.of(b.toDouble())
        val amountResult = if (result is Amount) result else Amount.of(result.toDouble())
        assertEquals(amountResult, amountA.subtract(amountB))
    }

    @ParameterizedTest
    @MethodSource("generator for times() works as expected")
    fun `times() works as expected`(
            a: Number,
            b: Number,
            result: Number,
    ) {
        val amountA = if (a is Amount) a else Amount.of(a.toDouble())
        val amountB = if (b is Amount) b else Amount.of(b.toDouble())
        val amountResult = if (result is Amount) result else Amount.of(result.toDouble())
        assertEquals(amountResult, amountA.times(amountB))
    }

    @ParameterizedTest
    @MethodSource("generator for divideBy() works as expected")
    fun `divideBy() works as expected`(
            a: Number,
            b: Number,
            result: Number,
    ) {
        val amountA = if (a is Amount) a else Amount.of(a.toDouble())
        val amountB = if (b is Amount) b else Amount.of(b.toDouble())
        val amountResult = if (result is Amount) result else Amount.of(result.toDouble())
        assertEquals(amountResult, amountA.divideBy(amountB))
    }

    @ParameterizedTest
    @MethodSource("generator for negate() works as expected")
    fun `negate() works as expected`(
            a: Number,
            result: Number,
    ) {
        val amountA = if (a is Amount) a else Amount.of(a.toDouble())
        val amountResult = if (result is Amount) result else Amount.of(result.toDouble())
        assertEquals(amountResult, amountA.negate())
    }

    @ParameterizedTest
    @MethodSource("generator for increasePercent() works as expected")
    fun `increasePercent() works as expected`(
            a: Number,
            b: Number,
            result: Number,
    ) {
        val amountA = if (a is Amount) a else Amount.of(a.toDouble())
        val amountB = if (b is Amount) b else Amount.of(b.toDouble())
        val amountResult = if (result is Amount) result else Amount.of(result.toDouble())
        assertEquals(amountResult, amountA.increasePercent(amountB))
    }

    @ParameterizedTest
    @MethodSource("generator for decreasePercent() works as expected")
    fun `decreasePercent() works as expected`(
            a: Number,
            b: Number,
            result: Number,
    ) {
        val amountA = if (a is Amount) a else Amount.of(a.toDouble())
        val amountB = if (b is Amount) b else Amount.of(b.toDouble())
        val amountResult = if (result is Amount) result else Amount.of(result.toDouble())
        assertEquals(amountResult, amountA.decreasePercent(amountB))
    }

    @ParameterizedTest
    @MethodSource("generator for percentageOf() works as expected")
    fun `percentageOf() works as expected`(
            a: Number,
            b: Number,
            result: Number,
    ) {
        val amountA = if (a is Amount) a else Amount.of(a.toDouble())
        val amountB = if (b is Amount) b else Amount.of(b.toDouble())
        val amountResult = if (result is Amount) result else Amount.of(result.toDouble())
        assertEquals(amountResult, amountA.percentageOf(amountB))
    }

    @ParameterizedTest
    @MethodSource("generator for percentageDifferenceOf() works as expected")
    fun `percentageDifferenceOf() works as expected`(
            a: Number,
            b: Number,
            result: Number,
    ) {
        val amountA = if (a is Amount) a else Amount.of(a.toDouble())
        val amountB = if (b is Amount) b else Amount.of(b.toDouble())
        val amountResult = if (result is Amount) result else Amount.of(result.toDouble())
        assertEquals(amountResult, amountA.percentageDifferenceOf(amountB))
    }

    @ParameterizedTest
    @MethodSource("generator for toPercent() works as expected")
    fun `toPercent() works as expected`(
            a: Number,
            result: Number,
    ) {
        val amountA = if (a is Amount) a else Amount.of(a.toDouble())
        val amountResult = if (result is Amount) result else Amount.of(result.toDouble())
        assertEquals(amountResult, amountA.toPercent())

    }

    @ParameterizedTest
    @MethodSource("generator for asDecimal() works as expected")
    fun `asDecimal() works as expected`(
            a: Number,
            result: Number,
    ) {
        val amountA = if (a is Amount) a else Amount.of(a.toDouble())
        val amountResult = if (result is Amount) result else Amount.of(result.toDouble())
        assertEquals(amountResult, amountA.asDecimal())
    }

    @ParameterizedTest
    @MethodSource("generator for remainder() works as expected")
    fun `remainder() works as expected`(
            a: Number,
            b: Number,
            result: Number,
    ) {
        val amountA = if (a is Amount) a else Amount.of(a.toDouble())
        val amountB = if (b is Amount) b else Amount.of(b.toDouble())
        val amountResult = if (result is Amount) result else Amount.of(result.toDouble())
        assertEquals(amountResult, amountA.remainder(amountB))
    }
}
