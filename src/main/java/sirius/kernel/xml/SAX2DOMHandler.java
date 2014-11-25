/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import org.w3c.dom.*;
import org.xml.sax.Attributes;
import sirius.kernel.commons.Strings;


/**
 * Internal adapter used to forward SAX events
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
class SAX2DOMHandler {

    private Document document;
    private Node root;
    private Node currentNode;
    private NodeHandler nodeHandler;

    protected SAX2DOMHandler(NodeHandler handler, Document document) {
        this.nodeHandler = handler;
        this.document = document;
    }

    private boolean nodeUp() {
        if (isComplete()) {
            nodeHandler.process(StructuredNode.of(root));
            return true;
        }
        currentNode = currentNode.getParentNode();
        return false;
    }

    private boolean isComplete() {
        return currentNode.equals(root);
    }

    private void createElement(String name, Attributes attributes) {
        Element element = document.createElement(name);
        for (int i = 0; i < attributes.getLength(); i++) {
            String attrName = attributes.getLocalName(i);
            if (Strings.isEmpty(attrName)) {
                attrName = attributes.getQName(i);
            }
            if (Strings.isFilled(attrName)) {
                element.setAttribute(attrName, attributes.getValue(i));
            }
        }
        if (currentNode != null) {
            currentNode.appendChild(element);
        } else {
            root = element;
            document.appendChild(element);
        }
        currentNode = element;
    }

    public Node getRoot() {
        return root;
    }

    protected void startElement(String uri, String name, Attributes attributes) {
        createElement(name, attributes);
    }

    protected void processingInstruction(String target, String data) {
        ProcessingInstruction instruction = document.createProcessingInstruction(target, data);
        currentNode.appendChild(instruction);
    }

    protected boolean endElement(String uri, String name) {
        if (!currentNode.getNodeName().equals(name)) {
            throw new DOMException(DOMException.SYNTAX_ERR,
                                   "Unexpected end-tag: " + name + " expected: " + currentNode.getNodeName());
        }
        return nodeUp();
    }

    protected void text(String data) {
        currentNode.appendChild(document.createTextNode(data));
    }

    protected NodeHandler getNodeHandler() {
        return nodeHandler;
    }
}
