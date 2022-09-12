/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import sirius.kernel.commons.ValueHolder;
import sirius.kernel.health.Counter;
import sirius.kernel.SiriusExtension;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SiriusExtension.class)
class XMLReaderTest {

    @Test
    @DisplayName("XMLReader extracts XPATH expression")
    void readXpath() throws Exception {
        ValueHolder<String> readString = ValueHolder.of(null);
        Counter nodeCount = new Counter();
        XMLReader reader = new XMLReader();
        reader.addHandler("test", node -> {
            nodeCount.inc();
            readString.set(node.queryString("value"));
        });

        reader.parse(new ByteArrayInputStream(
                "<doc><test><value>1</value></test><test><value>2</value></test><test><value>5</value></test></doc>".getBytes()));

        assertEquals("5", readString.get());
        assertEquals(3, nodeCount.getCount(), "parsed invalid count of nodes");
    }

    @Test
    @DisplayName("XMLReader supports compound XPATH paths")
    void readXpathCompound() throws Exception {
        ValueHolder<Boolean> shouldToggle = ValueHolder.of(false);
        ValueHolder<Boolean> shouldNotToggle = ValueHolder.of(false);
        XMLReader reader = new XMLReader();
        reader.addHandler("doc/test/value", node -> shouldToggle.set(true));
        reader.addHandler("value", node -> shouldNotToggle.set(true));

        reader.parse(new ByteArrayInputStream(
                "<doc><test><value>1</value></test><test><value>2</value></test><test><value>5</value></test></doc>".getBytes()));

        assertTrue(shouldToggle.get());
        assertFalse(shouldNotToggle.get());
    }

    @Test
    @DisplayName("XMLReader reads attributes")
    void readXpathAttributes() throws Exception {
        Map<String, String> attributes = new HashMap<>();
        ValueHolder<String> attribute = ValueHolder.of("");
        XMLReader reader = new XMLReader();
        reader.addHandler("test", node -> {
            attributes.putAll(node.getAttributes());
            attribute.set(node.getAttribute("namedAttribute").asString());
        });

        reader.parse(new ByteArrayInputStream("<doc><test namedAttribute=\"abc\" namedAttribute2=\"xyz\">1</test></doc>".getBytes()));

        assertEquals(2, attributes.size());
        assertEquals("abc", attribute.get());
    }

    @Test
    @DisplayName("reading non existing attributes does not throw errors")
    void readXpathMissingAttributes() throws Exception {
        Map<String, String> attributes = new HashMap<>();
        ValueHolder<String> attribute = ValueHolder.of("wrongValue");
        XMLReader reader = new XMLReader();
        reader.addHandler("test", node -> {
            attributes.putAll(node.getAttributes());
            attribute.set(node.getAttribute("namedAttribute").asString());
        });

        reader.parse(new ByteArrayInputStream("<doc><test>1</test></doc>".getBytes()));

        assertEquals(0, attributes.size());
        assertEquals("", attribute.get());
    }
}
