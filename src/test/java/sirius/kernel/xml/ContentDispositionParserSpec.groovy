/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml


import spock.lang.Specification

class ContentDispositionParserSpec extends Specification {

    def "parseContentDisposition regex works for different scenarios"() {
        expect:
        ContentDispositionParser.parseFileName(input).get() == output

        where:
        input                                                       | output
        "attachment; filename=\"test.pdf\""                         | "test.pdf"
        "inline; filename=test.pdf"                                 | "test.pdf"
        "attachment; filename=test.pdf ; size=\"2000\""             | "test.pdf"
        "attachment; size=\"2000\" ; filename=test.pdf"             | "test.pdf"
        "attachment; filename=\"test pdf doc.pdf\""                 | "test pdf doc.pdf"
        "inline; filename*=UTF-8''test.pdf"                         | "test.pdf"
        "inline; filename*=\"UTF-8''test.pdf\""                     | "test.pdf"
        "inline; filename*=\"UTF-8''test%20pdf%20doc.pdf\""         | "test pdf doc.pdf"
        "inline; filename*=UTF-8''test%20pdf%20doc.pdf"             | "test pdf doc.pdf"
        "attachment; filename*=iso-8859-1'en'file%27%20%27name.jpg" | "file' 'name.jpg"
    }
}
