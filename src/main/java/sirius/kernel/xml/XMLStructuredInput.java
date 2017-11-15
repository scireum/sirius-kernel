/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Represents an XML based input which can be processed using xpath.
 */
public class XMLStructuredInput implements StructuredInput {

    private StructuredNode node;

    /**
     * Creates a new XMLStructuredInput for the given stream.
     *
     * @param in    the InputStream containing the xml data.
     * @param close determines whether the stream should be closed after parsing or not
     * @throws IOException if an io error occurs while parsing the input xml
     */
    public XMLStructuredInput(InputStream in, boolean close) throws IOException {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(in);
            node = StructuredNode.of(doc.getDocumentElement());
            if (close) {
                in.close();
            }
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException(e);
        }
    }

    @Override
    public StructuredNode getNode(String xpath) throws XPathExpressionException {
        return node.queryNode(xpath);
    }

    @Override
    public String toString() {
        return node == null ? "" : node.toString();
    }

    /**
     * Overrides the root node to reset this document to a subtree of the original input
     *
     * @param node the new root node of this input
     */
    public void setNewParent(@Nonnull StructuredNode node) {
        if (node != null) {
            this.node = node;
        }
    }
}
