/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml


import spock.lang.Specification

class OutCallSpec extends Specification {

    def "parseContentDisposition regex works for different scenarios"() {
        expect:
        Outcall.parseFileName(input).get() == output

        where:
        input                                                                      | output
        "attachment; filename=\"test.pdf\""                                        | "test.pdf"
        "inline; filename=test.pdf"                                                | "test.pdf"
        "attachment; filename=test.pdf , size=\"2000\""                            | "test.pdf"
        "attachment; size=\"2000\" filename=test.pdf"                              | "test.pdf"
        "attachment; filename=\"test pdf doc.pdf\""                                | "test pdf doc.pdf"
        "form-data; name=\"fieldName\"; filename=\"filename.jpg\""                 | "filename.jpg"
    }
}
