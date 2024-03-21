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
import org.junit.jupiter.params.converter.ConvertWith
import org.junit.jupiter.params.provider.CsvSource
import sirius.kernel.SiriusExtension
import sirius.kernel.async.CallContext
import java.math.RoundingMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests the [Amount] class.
 */

@ExtendWith(SiriusExtension::class)
class AmountTest {

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
    @CsvSource(
        delimiter = '|', textBlock = """
        4.2            | 42             | 46.2
        42             | 4.2            | 46.2
        0              | 42             | 42 
        42             | 0              | 42
                       | 42             |  
        42             |                | 42 """
    )
    fun `add() works as expected`(
        @ConvertWith(AmountConverter::class) a: Amount,
        @ConvertWith(AmountConverter::class) b: Amount,
        @ConvertWith(AmountConverter::class) result: Amount,
    ) {

        assertEquals(result, a.add(b))
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|', textBlock = """
        4.2            | 42             | -37.8
        42             | 4.2            | 37.8
         0             | 42             | -42
        42             | 0              | 42
                       | 42             | 
        42             |                | 42 """
    )
    fun `subtract() works as expected`(
        @ConvertWith(AmountConverter::class) a: Amount,
        @ConvertWith(AmountConverter::class) b: Amount,
        @ConvertWith(AmountConverter::class) result: Amount,
    ) {

        assertEquals(result, a.subtract(b))
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|', textBlock = """
        4.2            | 42             | 176.4
        42             | 4.2            | 176.4
        0              | 42             | 0
        42             | 0              | 0
                       | 42             | 
        42             |                |   """
    )
    fun `times() works as expected`(
        @ConvertWith(AmountConverter::class) a: Amount,
        @ConvertWith(AmountConverter::class) b: Amount,
        @ConvertWith(AmountConverter::class) result: Amount,
    ) {
        assertEquals(result, a.times(b))
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|', textBlock = """
        4.2            | 42             | 0.1
        42             | 4.2            | 10
        0              | 42             | 0
        42             | 0              | 
                       | 42             | 
        42             |                |    """
    )
    fun `divideBy() works as expected`(
        @ConvertWith(AmountConverter::class) a: Amount,
        @ConvertWith(AmountConverter::class) b: Amount,
        @ConvertWith(AmountConverter::class) result: Amount,
    ) {
        assertEquals(result, a.divideBy(b))
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|', textBlock = """
        4.2            | -4.2
        -4.2           | 4.2
        42             | -42
        0              | 0
        -0             | 0
                       |    """
    )
    fun `negate() works as expected`(
        @ConvertWith(AmountConverter::class) a: Amount,
        @ConvertWith(AmountConverter::class) result: Amount,
    ) {
        assertEquals(result, a.negate())
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|', textBlock = """
        4.2            | 10             | 4.62
        0              | 42             | 0
        42             | 0              | 42
                       | 42             | 
        42             |                | 42 """
    )
    fun `increasePercent() works as expected`(
        @ConvertWith(AmountConverter::class) a: Amount,
        @ConvertWith(AmountConverter::class) b: Amount,
        @ConvertWith(AmountConverter::class) result: Amount,
    ) {

        assertEquals(result, a.increasePercent(b))
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|', textBlock = """
        4.2            | 10             | 3.78
        0              | 42             | 0
        42             | 0              | 42
                       | 42             |
        42             |                | 42"""
    )
    fun `decreasePercent() works as expected`(
        @ConvertWith(AmountConverter::class) a: Amount,
        @ConvertWith(AmountConverter::class) b: Amount,
        @ConvertWith(AmountConverter::class) result: Amount,
    ) {

        assertEquals(result, a.decreasePercent(b))
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|', textBlock = """
        4.2            | 42             | 10
        0              | 42             | 0
        42             | 0              | 
                       | 42             | 
        42             |                | """
    )
    fun `percentageOf() works as expected`(
        @ConvertWith(AmountConverter::class) a: Amount,
        @ConvertWith(AmountConverter::class) b: Amount,
        @ConvertWith(AmountConverter::class) result: Amount,
    ) {

        assertEquals(result, a.percentageOf(b))
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|', textBlock = """
        4.62           | 4.2            | 10
        0              | 42             | -100
        42             | 0              | 
                       | 42             | 
        42             |                |  """
    )
    fun `percentageDifferenceOf() works as expected`(
        @ConvertWith(AmountConverter::class) a: Amount,
        @ConvertWith(AmountConverter::class) b: Amount,
        @ConvertWith(AmountConverter::class) result: Amount,
    ) {

        assertEquals(result, a.percentageDifferenceOf(b))
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|', textBlock = """
        0.42           | 42
        1              | 100
        2              | 200
        0              | 0
                       |  """
    )
    fun `toPercent() works as expected`(
        @ConvertWith(AmountConverter::class) a: Amount,
        @ConvertWith(AmountConverter::class) result: Amount,
    ) {

        assertEquals(result, a.toPercent())

    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|', textBlock = """
        42                 | 0.42
        100                | 1
        200                | 2
        0                  | 0
                           |  """
    )
    fun `asDecimal() works as expected`(
        @ConvertWith(AmountConverter::class) a: Amount,
        @ConvertWith(AmountConverter::class) result: Amount,
    ) {

        assertEquals(result, a.asDecimal())
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|', textBlock = """
        10             | 2              | 0
        10             | 3              | 1
        10             | 0              | 
        0              | 10             | 0
                       | 10             | 
        10             |                |  """
    )
    fun `remainder() works as expected`(
        @ConvertWith(AmountConverter::class) a: Amount,
        @ConvertWith(AmountConverter::class) b: Amount,
        @ConvertWith(AmountConverter::class) result: Amount,
    ) {

        assertEquals(result, a.remainder(b))
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|', textBlock = """
        0.420          | 0.42
        1.0            | 1
        200            | 200
        200.0000       | 200
        600.010        | 600.01"""
    )
    fun `getAmountWithoutTrailingZeros() works as expected`(
        @ConvertWith(AmountConverter::class) a: Amount,
        result: String,
    ) {
        assertEquals(result, a.fetchAmountWithoutTrailingZeros()?.toPlainString())
    }
}
