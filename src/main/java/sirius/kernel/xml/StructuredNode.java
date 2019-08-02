/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import com.google.common.base.Charsets;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import sirius.kernel.cache.Cache;
import sirius.kernel.cache.CacheManager;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Tuple;
import sirius.kernel.commons.Value;
import sirius.kernel.health.Exceptions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Represents a structured node, which is part of a {@link StructuredInput}.
 * <p>
 * This is basically a XML node which can be queried using xpath.
 */
public class StructuredNode {

    /**
     * Cache to improve speed of xpath...
     */
    private static Cache<Tuple<Thread, String>, XPathExpression> cache;
    private static final XPathFactory XPATH = XPathFactory.newInstance();

    private Node node;

    /**
     * Wraps the given node
     *
     * @param root the node to wrap
     */
    protected StructuredNode(Node root) {
        node = root;
    }

    /*
     * Compiles the given xpath by utilizing the internal cache
     */
    private static XPathExpression compile(String xpath) throws XPathExpressionException {
        Tuple<Thread, String> key = Tuple.create(Thread.currentThread(), xpath);
        if (cache == null) {
            cache = CacheManager.createLocalCache("xpath");
        }
        XPathExpression result = cache.get(key);
        if (result == null) {
            result = XPATH.newXPath().compile(xpath);
            cache.put(key, result);
        }
        return result;
    }

    /**
     * Wraps the given W3C node into a structured node.
     *
     * @param node the node to wrap
     * @return a wrapped instance of the given node
     */
    @Nonnull
    public static StructuredNode of(@Nonnull Node node) {
        return new StructuredNode(node);
    }

    /**
     * Returns the current nodes name.
     *
     * @return returns the name of the node represented by this object
     */
    @Nonnull
    public String getNodeName() {
        return node.getNodeName();
    }

    /**
     * Returns the underlying W3C Node.
     *
     * @return the underlying node
     */
    @Nonnull
    public Node getNode() {
        return node;
    }

    /**
     * Determines if the underlying node is actually an instance of the given class.
     *
     * @param type the class to check for
     * @param <N>  the node type to check for
     * @return <tt>true</tt> if the underlying node is an instance of the given class, <tt>false</tt> otherwise
     */
    public <N extends Node> boolean is(Class<N> type) {
        return type.isInstance(node);
    }

    /**
     * Returns the underlying node casted to the given type.
     * <p>
     * Used {@link #is(Class)} to check if the node actually is an instance of the target class. Otherwise a
     * <tt>ClassCastException</tt> will be thrown.
     *
     * @param type the target class for the cast
     * @param <N>  the node type to cast to
     * @return the underlying node casted to the target type.
     * @throws java.lang.ClassCastException if the underlying node isn't an instance of the given class.
     */
    @SuppressWarnings("unchecked")
    public <N extends Node> N as(Class<N> type) {
        return (N) node;
    }

    /**
     * Returns a list of all children of this DOM node.
     *
     * @return a list containing all children of this node. If no children exist, an empty list will be returned.
     */
    @Nonnull
    public List<StructuredNode> getChildren() {
        NodeList result = node.getChildNodes();
        List<StructuredNode> resultList = new ArrayList<>(result.getLength());
        for (int i = 0; i < result.getLength(); i++) {
            resultList.add(new StructuredNode(result.item(i)));
        }
        return resultList;
    }

    /**
     * Returns a map of all attribute values of this DOM node with their names as keys.
     *
     * @return a map containing all attributes of this node. If no attributes exist, an empty map will be returned.
     */
    @Nonnull
    public Map<String, String> getAttributes() {
        Map<String, String> attributes = new HashMap<>();
        NamedNodeMap result = node.getAttributes();
        if (result != null) {
            for (int i = 0; i < result.getLength(); i++) {
                attributes.put(result.item(i).getNodeName(), result.item(i).getNodeValue());
            }
        }
        return attributes;
    }

    /**
     * Returns the value of the attribute with the given name.
     *
     * @param name the name of the attribute to read
     * @return a {@link Value} filled with the attribute value if an attribute exists for the given name, an empty
     * {@link Value} otherwise.
     */
    @Nonnull
    public Value getAttribute(String name) {
        NamedNodeMap attributes = getNode().getAttributes();
        if (attributes != null) {
            Node attribute = attributes.getNamedItem(name);
            if (attribute != null) {
                return Value.of(attribute.getNodeValue());
            }
        }
        return Value.EMPTY;
    }

    /**
     * Returns a given node at the relative path.
     *
     * @param xpath the xpath used to retrieve the resulting node
     * @return the node returned by the given xpath expression
     * @throws IllegalArgumentException if an invalid xpath was given
     */
    @Nullable
    public StructuredNode queryNode(String xpath) {
        try {
            Node result = (Node) compile(xpath).evaluate(node, XPathConstants.NODE);
            if (result == null) {
                return null;
            }

            return new StructuredNode(result);
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Returns a list of nodes at the relative path.
     *
     * @param xpath the xpath used to retrieve the resulting nodes
     * @return the list of nodes returned by the given xpath expression
     * @throws IllegalArgumentException if an invalid xpath was given
     */
    @Nonnull
    public List<StructuredNode> queryNodeList(String xpath) {
        try {
            NodeList result = (NodeList) compile(xpath).evaluate(node, XPathConstants.NODESET);
            List<StructuredNode> resultList = new ArrayList<>(result.getLength());
            for (int i = 0; i < result.getLength(); i++) {
                resultList.add(new StructuredNode(result.item(i)));
            }
            return resultList;
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Returns the property at the given relative path as string.
     *
     * @param path the xpath used to retrieve property
     * @return a string representation of the value returned by the given xpath expression
     * @throws IllegalArgumentException if an invalid xpath was given
     */
    @Nullable
    public String queryString(String path) {
        try {
            Object result = compile(path).evaluate(node, XPathConstants.NODE);
            if (result == null) {
                return null;
            }
            if (result instanceof Node) {
                String s = ((Node) result).getTextContent();
                if (s != null) {
                    return s.trim();
                }
                return s;
            }
            return result.toString().trim();
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Queries a {@link sirius.kernel.commons.Value} by evaluating the given xpath.
     *
     * @param path the xpath used to retrieve property
     * @return a Value wrapping the value returned by the given xpath expression
     * @throws java.lang.IllegalArgumentException if an invalid xpath was given
     */
    @Nonnull
    public Value queryValue(String path) {
        return Value.of(queryString(path));
    }

    /**
     * Queries a string via the given XPath. All XML is converted to a string.
     *
     * @param path the xpath used to retrieve the xml sub tree
     * @return a string representing the xml sub-tree returned by the given xpath expression
     * @throws IllegalArgumentException if an invalid xpath was given
     */
    @Nullable
    public String queryXMLString(String path) {
        return queryXMLString(path, true);
    }

    /**
     * Queries a string via the given XPath. All inner XML is converted to a string.
     *
     * @param path the xpath used to retrieve the xml sub tree
     * @return a string representing the xml sub-tree returned by the given xpath expression
     * @throws IllegalArgumentException if an invalid xpath was given
     */
    @Nullable
    public String queryInnerXMLString(String path) {
        return queryXMLString(path, false);
    }

    @Nullable
    private String queryXMLString(String path, boolean includeOuter) {
        try {
            XPath xpath = XPATH.newXPath();
            Object result = xpath.evaluate(path, node, XPathConstants.NODE);
            if (result == null) {
                return null;
            }
            if (result instanceof Node) {
                return serializeNodeAsXML((Node) result, includeOuter);
            }
            return result.toString().trim();
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private String serializeNodeAsXML(Node result, boolean includeOuter) {
        try {
            StringWriter writer = new StringWriter();
            XMLGenerator.writeXML(result, writer, Charsets.UTF_8.name(), true, includeOuter);
            return writer.toString();
        } catch (Exception e) {
            Exceptions.handle(e);
            return null;
        }
    }

    /**
     * Checks whether a node or non-empty content is reachable via the given
     * XPath.
     *
     * @param path the xpath to be checked
     * @return <tt>true</tt> if a node or non empty property was found, <tt>false</tt> otherwise
     * @throws IllegalArgumentException if an invalid xpath was given
     */
    public boolean isFilled(String path) {
        return Strings.isFilled(queryString(path));
    }

    /**
     * Checks whether a node is not reachable or has empty content via the given
     * XPath.
     *
     * @param path the xpath to be checked
     * @return <tt>true</tt> if no node or a empty property was found, <tt>false</tt> otherwise
     * @throws IllegalArgumentException if an invalid xpath was given
     */
    public boolean isEmpty(String path) {
        return Strings.isEmpty(queryString(path));
    }

    /**
     * Iterates through the sub-tree and invokes the given handler for each child node.
     *
     * @param nodeHandler the handler invoked for each child element
     */
    public void visitNodes(Consumer<StructuredNode> nodeHandler) {
        visit(nodeHandler, null);
    }

    /**
     * Iterates through the sub-tree and invokes the given handler for each text node.
     *
     * @param textNodeHandler the handler invoked for each text node
     */
    public void visitTexts(Consumer<Node> textNodeHandler) {
        visit(null, textNodeHandler);
    }

    /**
     * Iterates through the sub-tree and invokes the appropriate handler for each child node.
     *
     * @param nodeHandler     the handler invoked for each element node
     * @param textNodeHandler the handler invoked for each text node
     */
    public void visit(@Nullable Consumer<StructuredNode> nodeHandler, @Nullable Consumer<Node> textNodeHandler) {
        if (node.getNodeType() == Node.TEXT_NODE) {
            if (textNodeHandler != null) {
                textNodeHandler.accept(node);
            }
        } else if (node.getNodeType() == Node.ELEMENT_NODE) {
            if (nodeHandler != null) {
                nodeHandler.accept(this);
            }
            getChildren().forEach(c -> c.visit(nodeHandler, textNodeHandler));
        }
    }

    @Override
    public String toString() {
        try {
            StringWriter writer = new StringWriter();
            XMLGenerator.writeXML(node, writer, Charsets.UTF_8.name(), true);
            return writer.toString();
        } catch (Exception e) {
            Exceptions.handle(e);
            return node.toString();
        }
    }
}
