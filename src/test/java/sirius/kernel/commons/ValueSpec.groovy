package sirius.kernel.commons

import sirius.kernel.BaseSpecification
import sirius.kernel.nls.NLS

import java.time.LocalDate
import java.time.LocalDateTime

class ValueSpec extends BaseSpecification {

    private static final BigDecimal DEFAULT_BIG_DECIMAL = BigDecimal.TEN

    def "Test isFilled"() {
        expect:
        Value.of(input).isFilled() == output
        where:
        input  | output
        1      | true
        " "    | true
        "Test" | true
        ""     | false
        null   | false
    }

    def "Test isNumeric"() {
        expect:
        Value.of(input).isNumeric() == output
        where:
        input   | output
        1       | true
        "1"     | true
        -1      | true
        "-1"    | true
        0       | true
        "0"     | true
        1.1     | true
        "1.1"   | true
        "1.1.1" | false
        ""      | false
        null    | false
        "Test"  | false
    }

    def "Test afterLast"() {
        expect:
        Value.of(input).afterLast(separator) == output
        where:
        input        | separator | output
        "test.x.pdf" | "."       | "pdf"
    }

    def "Test beforeLast"() {
        expect:
        Value.of(input).beforeLast(separator) == output
        where:
        input        | separator | output
        "test.x.pdf" | "."       | "test.x"
    }

    def "Test afterFirst"() {
        expect:
        Value.of(input).afterFirst(separator) == output
        where:
        input        | separator | output
        "test.x.pdf" | "."       | "x.pdf"
    }

    def "Test beforeFirst"() {
        expect:
        Value.of(input).beforeFirst(separator) == output
        where:
        input        | separator | output
        "test.x.pdf" | "."       | "test"
    }

    def "Test left"() {
        expect:
        Value.of(input).left(length) == output
        where:
        input         | length | output
        "testA.testB" | 5      | "testA"
        "testA.testB" | -5     | ".testB"
        "test"        | 5      | "test"
        null          | 5      | ""
    }

    def "Test right"() {
        expect:
        Value.of(input).right(length) == output
        where:
        input         | length | output
        "testA.testB" | 5      | "testB"
        "testA.testB" | -5     | "testA."
        "test"        | 5      | "test"
        null          | 5      | ""
    }

    def "Test getBigDecimal"() {
        expect:
        Value.of(input).getBigDecimal() == output
        where:
        input               | output
        ""                  | null
        "Not a Number"      | null
        "42"                | BigDecimal.valueOf(42)
        "42.0"              | BigDecimal.valueOf(42)
        "42,0"              | BigDecimal.valueOf(42)
        42                  | BigDecimal.valueOf(42)
        42.0                | BigDecimal.valueOf(42)
        Integer.valueOf(42) | BigDecimal.valueOf(42)
    }

    def "Test getBigDecimal with default"() {
        expect:
        Value.of(input).getBigDecimal(DEFAULT_BIG_DECIMAL) == output
        where:
        input               | output
        ""                  | DEFAULT_BIG_DECIMAL
        "Not a Number"      | DEFAULT_BIG_DECIMAL
        "42"                | BigDecimal.valueOf(42)
        "42.0"              | BigDecimal.valueOf(42)
        "42,0"              | BigDecimal.valueOf(42)
        42                  | BigDecimal.valueOf(42)
        42.0                | BigDecimal.valueOf(42)
        Integer.valueOf(42) | BigDecimal.valueOf(42)
    }

    def "Test asBoolean with default"() {
        expect:
        Value.of(input).asBoolean(false) == output
        where:
        input              | output
        ""                 | false
        "false"            | false
        "true"             | true
        false              | false
        true               | true
        NLS.get("NLS.yes") | true
        NLS.get("NLS.no")  | false
    }


    def "Test asLocalDate with default"() {
        expect:
        Value.of(input).asLocalDate(null) == output
        where:
        input                                | output
        ""                                   | null
        LocalDate.of(1994, 5, 8)             | LocalDate.of(1994, 5, 8)
        LocalDateTime.of(1994, 5, 8, 10, 30) | LocalDate.of(1994, 5, 8)
        1537539103268                        | LocalDate.of(2018, 9, 21)
    }

    def "Test coerce boolean and without default"() {
        expect:
        Value.of(input).coerce(boolean.class, null) == output
        where:
        input              | output
        ""                 | false
        "false"            | false
        "true"             | true
        false              | false
        true               | true
        NLS.get("NLS.yes") | true
        NLS.get("NLS.no")  | false
    }

    def "Test translate works as expected"() {
        expect:
        Value.of(input).translate(lang).get() == output
        where:
        input                    | output                   | lang
        'regular string'         | "regular string"         | "de"
        '$nls.test.translate'    | "übersetzungs test"      | "de"
        '$nls.test.translate'    | "translation test"       | "en"
        LocalDate.of(1999, 1, 1) | LocalDate.of(1999, 1, 1) | null
        ["test1", "test2"]       | ["test1", "test2"]       | null
    }

    def "Boxing and retrieving an amount works"() {
        expect:
        Value.of(Amount.of(0.00001)).getAmount() == Amount.of(0.00001)
    }
    
    def "map() does not call the mapper on an empty Value"() {
        given:
        def count = 0
        def mapper = { value ->
            count++
            ""
        }
        when:
        Value.EMPTY.map(mapper)
        then:
        count == 0
    }

    def "flatMap() does not call the mapper on an empty Value"() {
        given:
        def count = 0
        def mapper = { value ->
            count++
            Optional.empty()
        }
        when:
        Value.EMPTY.flatMap(mapper)
        then:
        count == 0
    }
}
