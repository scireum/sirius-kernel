/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons

import sirius.kernel.BaseSpecification

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

    def "changing the lineSeparator works"() {
        given:
        StringWriter output = new StringWriter()
        and:
        CSVWriter writer = new CSVWriter(output).withLineSeparator("\r\n")
        when:
        writer.writeArray("a", "b", "c")
        writer.writeArray(1, 2, 3)
        writer.writeList(Arrays.asList("d", "e", "f"))
        and:
        output.close()
        then:
        output.toString() == "a;b;c\r\n1;2;3\r\nd;e;f"
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

    def "escaping of quotation works when using quotation"() {
        given:
        StringWriter output = new StringWriter()
        and:
        CSVWriter writer = new CSVWriter(output)
        when:
        writer.writeArray('"a"\nb')
        writer.writeArray('"a";b')
        then:
        and:
        output.close()
        then:
        output.toString() == '"\\"a\\"\nb"\n"\\"a\\";b"'
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

    def "escaping of separator with escape-char works if there is no quotation-char"() {
        given:
        StringWriter output = new StringWriter()
        and:
        CSVWriter writer = new CSVWriter(output)
        writer.withQuotation('\0' as char)
        when:
        writer.writeArray("a;b")
        and:
        output.close()
        then:
        output.toString() == 'a\\;b'
    }

    def "throw an exception if we have to escape quotes, but there is no escape-char"() {
        given:
        StringWriter output = new StringWriter()
        and:
        CSVWriter writer = new CSVWriter(output)
        writer.withEscape('\0' as char)
        when:
        writer.writeArray('"a";b')
        and:
        output.close()
        then:
        def e = thrown(IllegalArgumentException)
        e.getMessage() == "Cannot output a quotation character within a quoted string without an escape character."
    }

    def "throw an exception if there is a separator in the text, but there is no quotation-char and no escape-char"() {
        given:
        StringWriter output = new StringWriter()
        and:
        CSVWriter writer = new CSVWriter(output)
        writer.withQuotation('\0' as char)
        writer.withEscape('\0' as char)
        when:
        writer.writeArray('a;b')
        and:
        output.close()
        then:
        def e = thrown(IllegalArgumentException)
        e.getMessage() == "Cannot output a column which contains the separator character ';' without an escape or quotation character."
    }

    def "throw an exception if there is a new line in the text, but there is no quotation-char"() {
        given:
        StringWriter output = new StringWriter()
        and:
        CSVWriter writer = new CSVWriter(output)
        writer.withQuotation('\0' as char)
        when:
        writer.writeArray('a\nb')
        and:
        output.close()
        then:
        def e = thrown(IllegalArgumentException)
        e.getMessage() == "Cannot output a column which contains a line break without an quotation character."
    }
}
