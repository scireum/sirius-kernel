/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons

import sirius.kernel.BaseSpecification

import java.util.function.Consumer

class CSVWriterSpec extends BaseSpecification {

    def "simple data is output as CSV"() {
        given:
        StringWriter output = new StringWriter()
        and:
        CSVWriter writer = new CSVWriter(output)
        when:
        writer.writeArray("a", "b", "c")
        writer.writeArray(1, 2, 3)
        writer.writeList(Arrays.asList("d", "e", "f"))
        and:
        output.close()
        then:
        output.toString() == "a;b;c\n1;2;3\nd;e;f"
    }

    def "quotation works for separator and new line"() {
        given:
        StringWriter output = new StringWriter()
        and:
        CSVWriter writer = new CSVWriter(output)
        when:
        writer.writeArray("a;", "b", "c")
        and:
        writer.writeArray("1", "2\n2", "3")
        and:
        output.close()
        then:
        output.toString() == '"a;";b;c\n1;"2\n2";3'
    }

    def "escaping works for escape character and quotation"() {
        given:
        StringWriter output = new StringWriter()
        and:
        CSVWriter writer = new CSVWriter(output)
        when:
        writer.writeArray("a;b\"", "\\", "c")
        and:
        output.close()
        then:
        output.toString() == '"a;b\\"";\\\\;c'
    }

}
