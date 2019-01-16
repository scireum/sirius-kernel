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

class CSVReaderSpec extends BaseSpecification {

    def "valid CSV data can be parsed"() {
        given:
        def data = "a;b;c\n1;2;3\r\n4;5;6"
        when:
        List<Values> output = []
        and:
        new CSVReader(new StringReader(data)).execute(({ row -> output.add(row) } as Consumer<Values>))
        then:
        output.size() == 3
        and:
        output.get(0).at("A") == "a"
        and:
        output.get(0).at("B") == "b"
        and:
        output.get(0).at("C") == "c"
        and:
        output.get(1).at("A") == "1"
        and:
        output.get(1).at("B") == "2"
        and:
        output.get(1).at("C") == "3"
        and:
        output.get(2).at("A") == "4"
        and:
        output.get(2).at("B") == "5"
        and:
        output.get(2).at("C") == "6"

    }

    def "empty cells become an empty string"() {
        given:
        def data = "a;;c"
        when:
        List<Values> output = []
        and:
        new CSVReader(new StringReader(data)).execute(({ row -> output.add(row) } as Consumer<Values>))
        then:
        output.size() == 1
        and:
        output.get(0).at("A") == "a"
        and:
        output.get(0).at("B") == ""
        and:
        output.get(0).at("C") == "c"
    }

    def "quotation is detected and handled correctly"() {
        given:
        def data = '"a";"b;";1/4";d'
        when:
        List<Values> output = []
        and:
        new CSVReader(new StringReader(data)).execute(({ row -> output.add(row) } as Consumer<Values>))
        then:
        output.size() == 1
        and:
        output.get(0).at("A") == "a"
        and:
        output.get(0).at("B") == "b;"
        and:
        output.get(0).at("C") == '1/4"'
        and:
        output.get(0).at("D") == "d"
    }

    def "escaping works"() {
        given:
        def data = '\\"a;\\;;\\\\;x'
        when:
        List<Values> output = []
        and:
        new CSVReader(new StringReader(data)).execute(({ row -> output.add(row) } as Consumer<Values>))
        then:
        output.size() == 1
        and:
        output.get(0).at("A") == '"a'
        and:
        output.get(0).at("B") == ";"
        and:
        output.get(0).at("C") == "\\"
        and:
        output.get(0).at("D") == "x"
    }

    def "empty cells work with and without quotation"() {
        given:
        def data = 'a;;"";d'
        when:
        List<Values> output = []
        and:
        new CSVReader(new StringReader(data)).execute(({ row -> output.add(row) } as Consumer<Values>))
        then:
        output.size() == 1
        and:
        output.get(0).at("A") == 'a'
        and:
        output.get(0).at("B") == ""
        and:
        output.get(0).at("C") == ""
        and:
        output.get(0).at("D") == "d"
    }

    def "multiline values work"() {
        given:
        def data = '"a\nb";"c\r\nd";e\nf'
        when:
        List<Values> output = []
        and:
        new CSVReader(new StringReader(data)).execute(({ row -> output.add(row) } as Consumer<Values>))
        then:
        output.size() == 2
        and:
        output.get(0).at("A") == 'a\nb'
        and:
        output.get(0).at("B") == "c\r\nd"
        and:
        output.get(0).at("C") == "e"
        and:
        output.get(1).at("A") == "f"
    }

    def "ignoring whitespaces works"() {
        given:
        def data = '  a  ; "b" ;\t"c"\t; " \td\t ";\te\t;f '
        when:
        List<Values> output = []
        and:
        new CSVReader(new StringReader(data)).execute(({ row -> output.add(row) } as Consumer<Values>))
        then:
        output.size() == 1
        and:
        output.get(0).at("A") == '  a  '
        and:
        output.get(0).at("B") == "b"
        and:
        output.get(0).at("C") == "c"
        and:
        output.get(0).at("D") == " \td\t "
        and:
        output.get(0).at("E") == "\te\t"
        and:
        output.get(0).at("F") == "f "
    }

    def "modified settings work"() {
        given:
        def data = '!a! : !b! :&:c'
        when:
        List<Values> output = []
        and:
        new CSVReader(new StringReader(data))
                .withSeparator(':' as char)
                .withQuotation('!' as char)
                .withEscape('&' as char)
                .notIgnoringWhitespaces()
                .execute(({ row -> output.add(row) } as Consumer<Values>))
        then:
        output.size() == 1
        and:
        output.get(0).at("A") == 'a'
        and:
        output.get(0).at("B") == " !b! "
        and:
        output.get(0).at("C") == ":c"
    }

    def "quoted strings treat double quotes as escaped quote"() {
        given:
        def data = '"test""X""";"a";b""'
        when:
        List<Values> output = []
        and:
        new CSVReader(new StringReader(data))
                .execute(({ row -> output.add(row) } as Consumer<Values>))
        then:
        output.size() == 1
        and:
        output.get(0).at("A") == 'test"X"'
        and:
        output.get(0).at("B") == "a"
        and:
        output.get(0).at("C") == 'b""'
    }

    def "limit the number of line to read"() {
        given:
        def data = "a;b;c\n1;2;3\r\n4;5;6"
        def completeData = ""
        for (int i = 0; i < 100; i++) {
            completeData += data + "\n"
        }
        when:
        List<Values> output = []
        and:
        new CSVReader(new StringReader(completeData)).withLimit(new Limit(0, 100)).execute(({ row -> output.add(row) } as Consumer<Values>))
        then:
        output.size() == 100
    }

    def "skip and limit the number of line to read"() {
        given:
        def data = "a;b;c\n1;2;3\r\n4;5;6"
        def completeData = ""
        for (int i = 0; i < 100; i++) {
            completeData += data + "\n"
        }
        when:
        List<Values> output = []
        and:
        new CSVReader(new StringReader(completeData)).withLimit(new Limit(250, 100)).execute(({ row -> output.add(row) } as Consumer<Values>))
        then:
        output.size() == 50
    }

}
