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

    def "XMLReader extracts XPATH expression"() {
        given:
        def check = ValueHolder.of(null)
        def nodes = new Counter()
        def r = new XMLReader()
        and:
        r.addHandler("test", { n ->
            try {
                nodes.inc()
                check.set(n.queryString("value"))
            } catch (XPathExpressionException e) {
                throw Exceptions.handle(e)
            }
        } as NodeHandler)
        when:
        r.parse(new ByteArrayInputStream(
                "<doc><test><value>1</value></test><test><value>2</value></test><test><value>5</value></test></doc>".getBytes()))
        then:
        check.get() == "5"
        and:
        nodes.getCount() == 3
    }

    def "XMLReader reads attributes"() {
        given:
        def check = ValueHolder.of(null)
        def attributes
        def attribute
        def r = new XMLReader()
        and:
        r.addHandler("test", { n ->
            attributes = n.getAttributes()
            attribute = n.getAttribute("namedAttribute")
        } as NodeHandler)
        when:
        r.parse(new ByteArrayInputStream(
                "<doc><test namedAttribute=\"abc\" namedAttribute2=\"xyz\">1</test></doc>".getBytes()))
        then:
        attributes.size() == 2
        and:
        attribute.asString() == "abc"
    }

    def "reading non existing attributes does not throw errors"() {
        given:
        def check = ValueHolder.of(null)
        def attributes
        def attribute
        def r = new XMLReader()
        and:
        r.addHandler("test", { n ->
            attributes = n.getAttributes()
            attribute = n.getAttribute("namedAttribute")
        } as NodeHandler)
        when:
        r.parse(new ByteArrayInputStream(
                "<doc><test>1</test></doc>".getBytes()))
        then:
        attributes.size() == 0
        and:
        attribute.asString() == ""
    }
}
