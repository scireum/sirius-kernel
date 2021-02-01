/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

/**
 * Called by the {@link XMLReader} for a parsed sub-DOM tree.
 *
 * @see XMLReader
 */
public interface NodeHandler {

    /**
     * Invoked once a node was parsed. Depending on {@link #ignoreContent()} this will be the complete subtree or only
     * the node itself.
     *
     * @param node the root node of the subtree parsed by the SAX parser
     */
    void process(StructuredNode node);

    /**
     * Determines if the content of a node shall be handled. It can be used for root nodes of large files. There we do
     * not want to access the root node with all child nodes loaded into one node which would have a negative
     * performance impact.
     * <p>
     * When set to true, this handler will receive: "&lt;root someAttribute="abc"&gt;" without the child content from:
     * <pre>
     * &lt;root someAttribute="abc"&gt;
     *      someTextValue
     *      &lt;child>1&lt;/child&gt;
     *      ...
     *      &lt;child>10000000000&lt;/child&gt;
     * &lt;/root&gt;
     *
     * </pre>
     * When set to false, root with all child nodes will be received.
     *
     * @return <tt>true</tt> if the content shall be ignored, <tt>false</tt> otherwise
     */
    default boolean ignoreContent() {
        return false;
    }
}
