/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

/**
 * Represents structured data like XML or JSON which was read from a web service or other source.
 */
public interface StructuredInput {
    /**
     * Returns the node denoted by the given xpath expression
     *
     * @param xpath the xpath used to query the node
     * @return the node returned by the given xpath expression
     */
    StructuredNode getNode(String xpath);
}
