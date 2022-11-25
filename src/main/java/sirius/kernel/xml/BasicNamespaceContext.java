/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import sirius.kernel.commons.Strings;

import javax.xml.namespace.NamespaceContext;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Provides a simple implementation of {@link NamespaceContext} using an internal {@link Map}.
 */
public class BasicNamespaceContext implements NamespaceContext {

    private final Map<String, String> prefixToUri = new HashMap<>();

    /**
     * Registers a prefix and namespace URI.
     *
     * @param prefix the prefix to use
     * @param uri    the namespace URI to bind to the prefix
     * @return the context itself for fluent method calls
     */
    public BasicNamespaceContext withNamespace(String prefix, String uri) {
        this.prefixToUri.put(prefix, uri);
        return this;
    }

    /**
     * Returns all registered prefixes along with their namespace URIs.
     *
     * @return a stream of entries mapping prefixes to their namespace URIs
     */
    public Stream<Map.Entry<String, String>> getPrefixAndUris() {
        return prefixToUri.entrySet().stream();
    }

    @Override
    public String getNamespaceURI(String prefix) {
        return prefixToUri.get(prefix);
    }

    @Override
    public String getPrefix(String namespaceURI) {
        return prefixToUri.entrySet()
                          .stream()
                          .filter(entry -> Strings.areEqual(entry.getValue(), namespaceURI))
                          .map(Map.Entry::getKey)
                          .findFirst()
                          .orElse(null);
    }

    @Override
    public Iterator<String> getPrefixes(String namespaceURI) {
        return prefixToUri.entrySet()
                          .stream()
                          .filter(entry -> Strings.areEqual(entry.getValue(), namespaceURI))
                          .map(Map.Entry::getKey)
                          .toList()
                          .iterator();
    }
}
