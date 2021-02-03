/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

/**
 * Can be used to handle XML nodes without having the parser to read out the full content of the node, but only
 * the node with attributes. See {@link NodeHandler#ignoreContent()} for an example.
 */
public abstract class NoContentNodeHandler implements NodeHandler {

    @Override
    public boolean ignoreContent() {
        return true;
    }
}
