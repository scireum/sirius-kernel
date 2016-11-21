package sirius.kernel.commons

import spock.lang.Specification

class ValueSpec extends Specification {

    def "Test isFilled"() {
        expect:
        Value.of(input).isFilled() == output;
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
}
