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
import sirius.kernel.commons.Explain;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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
     * @param inputStream      the InputStream containing the xml data
     * @param namespaceContext the namespace context to use when applying XPATH queries
     * @throws IOException if an io error occurs while parsing the input xml
     */
    public XMLStructuredInput(InputStream inputStream, @Nullable NamespaceContext namespaceContext) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            if (namespaceContext != null) {
                factory.setNamespaceAware(true);
            }
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            this.node = StructuredNode.of(document.getDocumentElement(), namespaceContext);
        } catch (ParserConfigurationException | SAXException exception) {
            throw new IOException(exception);
        }
    }

    /**
     * Returns the document root.
     *
     * @return the root element wrapped as {@link StructuredNode}
     */
    public StructuredNode root() {
        return node;
    }

    @Override
    public StructuredNode getNode(String xpath) {
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
    @SuppressWarnings("squid:S2589")
    @Explain("We really want to ensure that the given value is not null and not just rely on annotation.")
    public void setNewParent(@Nonnull StructuredNode node) {
        if (node != null) {
            this.node = node;
        }
    }
}
