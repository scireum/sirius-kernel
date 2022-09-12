/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.nls

import sirius.kernel.BaseSpecification
import sirius.kernel.commons.Context

class FormatterSpec extends BaseSpecification {

    def "format replaces parameters"() {
        given:
        def pattern = "Test \${foo}"
        when:
        def result = Formatter.create(pattern).set("foo", "bar").format()
        then:
        result == "Test bar"
    }

    def "set trims and calls toUserString on parameters"() {
        given:
        def pattern = "Test \${foo} \${bar}"
        when:
        def result = Formatter.create(pattern).set("foo", true).set("bar", " test ").format()
        then:
        result == "Test Ja test"
    }

    def "setDirect neither trims nor calls toUserString on parameters"() {
        given:
        def pattern = "Test \${foo} \${bar}"
        when:
        def result = Formatter.
                create(pattern).
                setDirect(Context.create().set("foo", true)).
                setDirect("bar", " test ").
                format()
        then:
        result == "Test true  test "
    }

    def "smartFormat skips empty block"() {
        given:
        def pattern = "Test[ \${foo}]"
        when:
        def result = Formatter.create(pattern).set("foo", null).smartFormat()
        then:
        result == "Test"
    }

    def "smartFormat accepts nested blocks"() {
        given:
        def pattern = "Test[[ \${foo}] bar \${baz}]"
        when:
        def result1 = Formatter.create(pattern).set("foo", null).set("baz", null).smartFormat()
        def result2 = Formatter.create(pattern).set("foo", "foo").set("baz", null).smartFormat()
        def result3 = Formatter.create(pattern).set("foo", null).set("baz", "baz").smartFormat()
        def result4 = Formatter.create(pattern).set("foo", "foo").set("baz", "baz").smartFormat()
        then:
        result1 == "Test"
        result2 == "Test foo bar "
        result3 == "Test bar baz"
        result4 == "Test foo bar baz"
    }

    def "format fails when using unknown parameter"() {
        given:
        def pattern = "Test \${foo}"
        when:
        Formatter.create(pattern).smartFormat()
        then:
        thrown(IllegalArgumentException)
    }

    def "format fails for missing }"() {
        given:
        def pattern = "Test \${foo"
        when:
        Formatter.create(pattern).smartFormat()
        then:
        thrown(IllegalArgumentException)
    }

    def "format fails for missing ]"() {
        given:
        def pattern = "Test [\${foo}"
        when:
        Formatter.create(pattern).smartFormat()
        then:
        thrown(IllegalArgumentException)
    }

    def "smartFormat fails for additional ]"() {
        given:
        def pattern = "Test [\${foo}]]"
        when:
        Formatter.create(pattern).smartFormat()
        then:
        thrown(IllegalArgumentException)
    }

}
