/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml

import sirius.kernel.BaseSpecification
import sirius.kernel.commons.ValueHolder
import sirius.kernel.health.Counter
import sirius.kernel.health.Exceptions

import javax.xml.xpath.XPathExpressionException

class XMLReaderSpec extends BaseSpecification {


    public static
    final String TEST_XML = "<doc><test><value>1</value></test><test><value>2</value></test><test><value>5</value></test></doc>"

    def "XMLReader extracts XPATH expression"() {
        given:
        def check = ValueHolder.of(null)
        def nodes = new Counter()
        def r = new XMLReader()
        and:
        r.addHandler("test",
                { n ->
                    try {
                        nodes.inc()
                        check.set(n.queryString("value"))
                    } catch (XPathExpressionException e) {
                        throw Exceptions.handle(e)
                    }
                } as NodeHandler)
        when:
        r.parse(new ByteArrayInputStream(
                TEST_XML.getBytes()))
        then:
        check.get() == "5"
        and:
        nodes.getCount() == 3
    }


    def "XMLReader calls missingHandler"() {
        given:
        def check = ValueHolder.of(null)
        def missing = new Counter()
        def r = new XMLReader()
        and:
        r.addMissingHandler("notinxml",
                { ->
                    missing.inc()
                } as Runnable)
        when:
        r.parse(new ByteArrayInputStream(
                TEST_XML.getBytes()))
        then:
        missing.getCount() == 1
    }

    def "XMLReader does not call missingHandler if in xml"() {
        given:
        def check = ValueHolder.of(null)
        def nodes = new Counter()
        def missing = new Counter()
        def r = new XMLReader()
        and:
        r.addHandler("test",
                { n ->
                    try {
                        nodes.inc()
                        check.set(n.queryString("value"))
                    } catch (XPathExpressionException e) {
                        throw Exceptions.handle(e)
                    }
                } as NodeHandler,
                { ->
                    missing.inc()
                } as Runnable)
        when:
        r.parse(new ByteArrayInputStream(
                TEST_XML.getBytes()))
        then:
        check.get() == "5"
        and:
        nodes.getCount() == 3
        and:
        then:
        missing.getCount() == 0
    }
}
