package sirius.kernel.commons

import sirius.kernel.BaseSpecification
import sirius.kernel.async.CallContext

import java.util.function.Supplier

class AmountSpec extends BaseSpecification {

    def "predicates are evaluated correctly"() {
        expect:
        Amount.NOTHING.isEmpty()
        Amount.NOTHING.isZeroOrNull()
        Amount.ZERO.isZeroOrNull()
        Amount.ZERO.isZero()
        Amount.MINUS_ONE.isNonZero()
        Amount.MINUS_ONE.isNegative()
        Amount.TEN.isPositive()
        Amount.TEN > Amount.ONE
    }

    def "Amount.of converts various types correctly"() {
        when:
        CallContext.getCurrent().setLang("en")
        then:
        Amount.ONE == x

        where:
        x << [Amount.of(1.0D),
              Amount.of(1L),
              Amount.of(Integer.valueOf(1)),
              Amount.of(Long.valueOf(1L)),
              Amount.of(Double.valueOf(1D)),
              Amount.ofMachineString("1.0"),
              Amount.ofUserString("1.0")]
    }

    def "ofMachineString works correctly"() {
        expect:
        Amount.ofMachineString(input) == output

        where:
        input | output
        "1"   | Amount.ONE
        "0.1" | Amount.ONE.divideBy(Amount.TEN)
        "10"  | Amount.TEN
    }

    def "Computations with NOTHING result in expected values"() {
        expect:
        Amount.ONE == Amount.ONE.add(Amount.NOTHING)
        Amount.NOTHING == Amount.NOTHING.add(Amount.ONE)
        Amount.NOTHING == Amount.NOTHING.add(Amount.NOTHING)
        Amount.of(2) == Amount.ONE.add(Amount.ONE)
        Amount.MINUS_ONE == Amount.ONE.subtract(Amount.of(2))
        Amount.ONE_HUNDRED == Amount.TEN.times(Amount.TEN)
        Amount.NOTHING == Amount.TEN.times(Amount.NOTHING)
        Amount.NOTHING == Amount.NOTHING.times(Amount.TEN)
        Amount.NOTHING == Amount.ONE.divideBy(Amount.ZERO)
        Amount.NOTHING == Amount.ONE.divideBy(Amount.NOTHING)
        Amount.NOTHING == Amount.NOTHING.divideBy(Amount.ONE)
        Amount.TEN == Amount.ONE_HUNDRED.divideBy(Amount.TEN)
        Amount.of(2) == Amount.ONE.increasePercent(Amount.ONE_HUNDRED)
        Amount.of(0.5) == Amount.ONE.decreasePercent(Amount.of(50))
        Amount.of(19) == Amount.TEN.multiplyPercent(Amount.TEN)
    }

    def "fill and orElseGet are only evaluated if no value is present"() {
        expect:
        Amount.TEN == Amount.NOTHING.fill(Amount.TEN)
        Amount.TEN == Amount.TEN.fill(Amount.ONE)
        Amount.NOTHING.orElseGet(new Supplier<Amount>() {
            @Override
            Amount get() {
                return Amount.TEN
            }
        }) == Amount.TEN
    }

    def "helper functions compute correct values"() {
        expect:
        Amount.TEN == Amount.TEN.percentageOf(Amount.ONE_HUNDRED)
        Amount.ONE_HUNDRED == Amount.TEN.percentageDifferenceOf(Amount.of(5))
        Amount.of(-50) == Amount.of(5).percentageDifferenceOf(Amount.TEN)
        Amount.of(0.5) == Amount.of(50).asDecimal()
        Amount.ofMachineString("1.23") == Amount.ofMachineString("1.23223").round(NumberFormat.PERCENT)
        Amount.ONE_HUNDRED.getDigits() == 3
        Amount.ONE.getDigits() == 1
        Amount.ONE_HUNDRED.subtract(Amount.ONE).getDigits() == 2
        Amount.of(477).getDigits() == 3
    }

    def "formatting works as expected"() {
        when:
        CallContext.getCurrent().setLang("en")
        then:
        Amount.ofMachineString("0.1").toPercent().toPercentString() == "10 %"
        Amount.of(1.23).toRoundedString() == "1"
        Amount.of(1.00).toSmartRoundedString(NumberFormat.TWO_DECIMAL_PLACES).asString() == "1"
        Amount.of(1.00).toString(NumberFormat.TWO_DECIMAL_PLACES).asString() == "1.00"
        Amount.of(1.23).toSmartRoundedString(NumberFormat.TWO_DECIMAL_PLACES).asString() == "1.23"
        Amount.of(0.012).toScientificString(0, "") == "12 m"
        Amount.of(1200).toScientificString(1, "") == "1.2 K"
    }

    def "min selects the lower value and handles null and NOTHING gracefully"() {
        expect:
        Amount.ONE.min(Amount.TEN) == Amount.ONE
        Amount.TEN.min(Amount.ONE) == Amount.ONE
        Amount.NOTHING.min(Amount.ONE) == Amount.ONE
        Amount.ONE.min(Amount.NOTHING) == Amount.ONE
        Amount.NOTHING.min(Amount.NOTHING) == Amount.NOTHING
        Amount.TEN.min(null) == Amount.TEN
        Amount.ZERO.min(Amount.NOTHING) == Amount.ZERO
    }

    def "max selects the higher value and handles null and NOTHING gracefully"() {
        expect:
        Amount.ONE.max(Amount.TEN) == Amount.TEN
        Amount.TEN.max(Amount.ONE) == Amount.TEN
        Amount.NOTHING.max(Amount.ONE) == Amount.ONE
        Amount.ONE.max(Amount.NOTHING) == Amount.ONE
        Amount.NOTHING.max(Amount.NOTHING) == Amount.NOTHING
        Amount.TEN.max(null) == Amount.TEN
        Amount.ZERO.max(Amount.NOTHING) == Amount.ZERO
    }

    def "compare returns which amount is higher"() {
        expect:
        Amount.NOTHING.compareTo(Amount.NOTHING) == 0
        Amount.ONE.compareTo(Amount.NOTHING) > 0
        Amount.NOTHING.compareTo(Amount.ONE) < 0
        Amount.ONE.compareTo(Amount.ONE) == 0
        Amount.ONE.compareTo(Amount.MINUS_ONE) > 0
        Amount.MINUS_ONE.compareTo(Amount.ONE) < 0
    }

    def "boilerplace comparators work"() {
        expect:
        Amount.ONE.isGreaterThan(Amount.NOTHING)
        Amount.ONE.isGreaterThanOrEqual(Amount.NOTHING)
        Amount.NOTHING.isLessThan(Amount.ONE)
        Amount.NOTHING.isLessThanOrEqual(Amount.ONE)
        Amount.ONE.isGreaterThanOrEqual(Amount.ONE)
        Amount.ONE.isLessThanOrEqual(Amount.ONE)
        Amount.ONE.isGreaterThan(Amount.MINUS_ONE)
        Amount.MINUS_ONE.isLessThan(Amount.ONE)
    }

    def "add() works as expected"() {
        given:
        Amount amountA = a instanceof Amount ? a : Amount.of(a)
        Amount amountB = b instanceof Amount ? b : Amount.of(b)
        Amount amountResult = result instanceof Amount ? result : Amount.of(result)
        expect:
        amountA.add(amountB) == amountResult
        where:
        a              | b              | result
        4.2            | 42             | 46.2
        42             | 4.2            | 46.2
        Amount.ZERO    | 42             | 42
        42             | Amount.ZERO    | 42
        Amount.NOTHING | 42             | Amount.NOTHING
        42             | Amount.NOTHING | 42
    }

    def "subtract() works as expected"() {
        given:
        Amount amountA = a instanceof Amount ? a : Amount.of(a)
        Amount amountB = b instanceof Amount ? b : Amount.of(b)
        Amount amountResult = result instanceof Amount ? result : Amount.of(result)
        expect:
        amountA.subtract(amountB) == amountResult
        where:
        a              | b              | result
        4.2            | 42             | -37.8
        42             | 4.2            | 37.8
        Amount.ZERO    | 42             | -42
        42             | Amount.ZERO    | 42
        Amount.NOTHING | 42             | Amount.NOTHING
        42             | Amount.NOTHING | 42
    }

    def "times() works as expected"() {
        given:
        Amount amountA = a instanceof Amount ? a : Amount.of(a)
        Amount amountB = b instanceof Amount ? b : Amount.of(b)
        Amount amountResult = result instanceof Amount ? result : Amount.of(result)
        expect:
        amountA.times(amountB) == amountResult
        where:
        a              | b              | result
        4.2            | 42             | 176.4
        42             | 4.2            | 176.4
        Amount.ZERO    | 42             | Amount.ZERO
        42             | Amount.ZERO    | Amount.ZERO
        Amount.NOTHING | 42             | Amount.NOTHING
        42             | Amount.NOTHING | Amount.NOTHING
    }

    def "divideBy() works as expected"() {
        given:
        Amount amountA = a instanceof Amount ? a : Amount.of(a)
        Amount amountB = b instanceof Amount ? b : Amount.of(b)
        Amount amountResult = result instanceof Amount ? result : Amount.of(result)
        expect:
        amountA.divideBy(amountB) == amountResult
        where:
        a              | b              | result
        4.2            | 42             | 0.1
        42             | 4.2            | Amount.TEN
        Amount.ZERO    | 42             | Amount.ZERO
        42             | Amount.ZERO    | Amount.NOTHING
        Amount.NOTHING | 42             | Amount.NOTHING
        42             | Amount.NOTHING | Amount.NOTHING
    }

    def "negate() works as expected"() {
        given:
        Amount amountA = a instanceof Amount ? a : Amount.of(a)
        Amount amountResult = result instanceof Amount ? result : Amount.of(result)
        expect:
        amountA.negate() == amountResult
        where:
        a              | result
        4.2            | -4.2
        -4.2           | 4.2
        42             | -42
        Amount.ZERO    | Amount.ZERO
        -0             | Amount.ZERO
        Amount.NOTHING | Amount.NOTHING
    }

    def "increasePercent() works as expected"() {
        given:
        Amount amountA = a instanceof Amount ? a : Amount.of(a)
        Amount amountB = b instanceof Amount ? b : Amount.of(b)
        Amount amountResult = result instanceof Amount ? result : Amount.of(result)
        expect:
        amountA.increasePercent(amountB) == amountResult
        where:
        a              | b              | result
        4.2            | Amount.TEN     | 4.62
        Amount.ZERO    | 42             | Amount.ZERO
        42             | Amount.ZERO    | 42
        Amount.NOTHING | 42             | Amount.NOTHING
        42             | Amount.NOTHING | 42
    }

    def "decreasePercent() works as expected"() {
        given:
        Amount amountA = a instanceof Amount ? a : Amount.of(a)
        Amount amountB = b instanceof Amount ? b : Amount.of(b)
        Amount amountResult = result instanceof Amount ? result : Amount.of(result)
        expect:
        amountA.decreasePercent(amountB) == amountResult
        where:
        a              | b              | result
        4.2            | Amount.TEN     | 3.78
        Amount.ZERO    | 42             | Amount.ZERO
        42             | Amount.ZERO    | 42
        Amount.NOTHING | 42             | Amount.NOTHING
        42             | Amount.NOTHING | 42
    }

    def "multiplyPercent() works as expected"() {
        given:
        Amount amountA = a instanceof Amount ? a : Amount.of(a)
        Amount amountB = b instanceof Amount ? b : Amount.of(b)
        Amount amountResult = result instanceof Amount ? result : Amount.of(result)
        expect:
        amountA.multiplyPercent(amountB) == amountResult
        where:
        a              | b              | result
        4.2            | Amount.TEN     | 13.78
        Amount.ZERO    | 42             | 42
        42             | Amount.ZERO    | 42
        Amount.NOTHING | 42             | Amount.NOTHING
        42             | Amount.NOTHING | 42
    }

    def "percentageOf() works as expected"() {
        given:
        Amount amountA = a instanceof Amount ? a : Amount.of(a)
        Amount amountB = b instanceof Amount ? b : Amount.of(b)
        Amount amountResult = result instanceof Amount ? result : Amount.of(result)
        expect:
        amountA.percentageOf(amountB) == amountResult
        where:
        a              | b              | result
        4.2            | 42             | Amount.TEN
        Amount.ZERO    | 42             | Amount.ZERO
        42             | Amount.ZERO    | Amount.NOTHING
        Amount.NOTHING | 42             | Amount.NOTHING
        42             | Amount.NOTHING | Amount.NOTHING
    }

    def "percentageDifferenceOf() works as expected"() {
        given:
        Amount amountA = a instanceof Amount ? a : Amount.of(a)
        Amount amountB = b instanceof Amount ? b : Amount.of(b)
        Amount amountResult = result instanceof Amount ? result : Amount.of(result)
        expect:
        amountA.percentageDifferenceOf(amountB) == amountResult
        where:
        a              | b              | result
        4.62           | 4.2            | Amount.TEN
        Amount.ZERO    | 42             | -100
        42             | Amount.ZERO    | Amount.NOTHING
        Amount.NOTHING | 42             | Amount.NOTHING
        42             | Amount.NOTHING | Amount.NOTHING
    }

    def "toPercent() works as expected"() {
        given:
        Amount amountA = a instanceof Amount ? a : Amount.of(a)
        Amount amountResult = result instanceof Amount ? result : Amount.of(result)
        expect:
        amountA.toPercent() == amountResult
        where:
        a              | result
        0.42           | 42
        Amount.ONE     | Amount.ONE_HUNDRED
        2              | 200
        Amount.ZERO    | Amount.ZERO
        Amount.NOTHING | Amount.NOTHING
    }

    def "asDecimal() works as expected"() {
        given:
        Amount amountA = a instanceof Amount ? a : Amount.of(a)
        Amount amountResult = result instanceof Amount ? result : Amount.of(result)
        expect:
        amountA.asDecimal() == amountResult
        where:
        a                  | result
        42                 | 0.42
        Amount.ONE_HUNDRED | Amount.ONE
        200                | 2
        Amount.ZERO        | Amount.ZERO
        Amount.NOTHING     | Amount.NOTHING
    }

    def "remainder() works as expected"() {
        given:
        Amount amountA = a instanceof Amount ? a : Amount.of(a)
        Amount amountB = b instanceof Amount ? b : Amount.of(b)
        Amount amountResult = result instanceof Amount ? result : Amount.of(result)
        expect:
        amountA.remainder(amountB) == amountResult
        where:
        a              | b              | result
        10             | 2              | 0
        10             | 3              | 1
        10             | 0              | Amount.NOTHING
        0              | 10             | 0
        Amount.NOTHING | 10             | Amount.NOTHING
        10             | Amount.NOTHING | Amount.NOTHING
    }
}
