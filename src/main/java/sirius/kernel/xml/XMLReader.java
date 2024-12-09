/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import sirius.kernel.async.TaskContext;
import sirius.kernel.commons.Strings;
import sirius.kernel.health.Exceptions;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * A combination of DOM and SAX parser which permits to parse very large XML files while conveniently handling subtree
 * using a DOM and xpath api.
 * <p>
 * Used SAX to parse a given XML file. A set of {@link NodeHandler} objects can be given, which get notified if
 * a subtree below a given tag was parsed. This subtree is available as DOM and can conveniently be processed
 * using xpath.
 */
public class XMLReader extends DefaultHandler {

    private final TaskContext taskContext;

    private final Map<String, NodeHandler> handlers = new TreeMap<>();
    private final List<SAX2DOMHandler> activeHandlers = new ArrayList<>();
    private final DocumentBuilder documentBuilder;
    private final List<String> currentPath = new ArrayList<>();

    /**
     * Creates a new XMLReader.
     * <p>
     * Use {@link #addHandler(String, NodeHandler)} to bind handlers to tags and then call one of the <tt>parse</tt>
     * methods to process the XML file.
     * <p>
     * To interrupt processing use {@link TaskContext#cancel()}.
     */
    public XMLReader() {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            taskContext = TaskContext.get();
        } catch (ParserConfigurationException exception) {
            throw Exceptions.handle(exception);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        // Delegate to active handlers...
        String cData = new String(ch).substring(start, start + length);
        for (SAX2DOMHandler handler : activeHandlers) {
            handler.text(cData);
        }
    }

    @Override
    public void endDocument() throws SAXException {
        // Consider iterating over all activeHandler which are not complete
        // yet and raise an exception.
        // For now this is simply ignored to make processing more robust.
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException {
        // Delegate to active handlers and deletes them if they are finished...
        activeHandlers.removeIf(handler -> handler.endElement(name));

        currentPath.removeLast();
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        // Delegate to active handlers...
        for (SAX2DOMHandler handler : activeHandlers) {
            handler.processingInstruction(target, data);
        }
    }

    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
        // Delegate to active handlers...
        for (SAX2DOMHandler handler : activeHandlers) {
            handler.createElement(name, attributes);
        }

        // Start a new handler if necessary
        currentPath.add(name);
        NodeHandler handler = handlers.get(Strings.join(currentPath, "/"));
        if (handler == null) {
            handler = handlers.get(name);
        }
        if (handler != null) {
            SAX2DOMHandler saxHandler = new SAX2DOMHandler(handler, documentBuilder.newDocument());
            saxHandler.createElement(name, attributes);
            if (!handler.ignoreContent()) {
                activeHandlers.add(saxHandler);
            }
        }

        // Check if the user tried to interrupt parsing....
        if (!taskContext.isActive()) {
            throw new UserInterruptException();
        }
    }

    /**
     * Registers a new handler for a qualified name of a node.
     * <p>
     * Note that this can be either the node name itself or it can be the path to the node separated by
     * "/". Therefore, &lt;foo&gt;&lt;bar&gt; would be matched by <tt>bar</tt> and by <tt>foo/bar</tt>, where
     * the path always has precedence over the single node name.
     * <p>
     * Handlers are invoked after the complete node was read. Namespaces are ignored for now which eases
     * the processing a lot (especially for xpath related tasks). Namespaces however
     * could be easily added by replacing String with QName here.
     *
     * @param name    the qualified name of the tag which should be parsed and processed
     * @param handler the NodeHandler used to process the parsed DOM subtree
     */
    public void addHandler(String name, NodeHandler handler) {
        handlers.put(name, handler);
    }

    /**
     * Parses the given stream.
     *
     * @param stream the stream to parse
     * @throws IOException if parsing the XML fails either due to an IO error or due to an SAXException (when
     *                     processing a malformed XML).
     */
    public void parse(InputStream stream) throws IOException {
        parse(stream, null);
    }

    /**
     * Used to handle an abort via {@link TaskContext}
     */
    static class UserInterruptException extends RuntimeException {

        @Serial
        private static final long serialVersionUID = -7454219131982518216L;
    }

    /**
     * Parses the given stream using the given locator and interrupt signal.
     *
     * @param stream          the stream containing the XML data
     * @param resourceLocator the resource locator used to discover dependent resources
     * @throws IOException if parsing the XML fails either due to an IO error or due to an SAXException (when
     *                     processing a malformed XML).
     */
    public void parse(InputStream stream, Function<String, InputStream> resourceLocator) throws IOException {
        try (stream) {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            SAXParser saxParser = factory.newSAXParser();
            org.xml.sax.XMLReader reader = saxParser.getXMLReader();
            reader.setEntityResolver(new EntityResolver() {

                @Override
                public InputSource resolveEntity(String publicId, String systemId) throws IOException {
                    return tryResolveEntity(systemId, resourceLocator);
                }
            });
            reader.setContentHandler(this);
            reader.parse(new InputSource(stream));
        } catch (ParserConfigurationException | SAXException exception) {
            throw new IOException(exception);
        } catch (UserInterruptException exception) {
            // IGNORED - this is used to cancel parsing if the used tried to
            // cancel a process.
        }
    }

    private InputSource tryResolveEntity(String systemId, Function<String, InputStream> resourceLocator)
            throws IOException {
        URL url = URI.create(systemId).toURL();
        if (!"file".equals(url.getProtocol())) {
            return emptyResource();
        }

        File file = new File(url.getFile());
        if (file.exists()) {
            return new InputSource(new FileInputStream(file));
        }

        if (resourceLocator == null) {
            return emptyResource();
        }

        InputStream stream = resourceLocator.apply(file.getName());
        if (stream != null) {
            return new InputSource(stream);
        }

        return emptyResource();
    }

    private InputSource emptyResource() {
        return new InputSource(new StringReader(""));
    }
}
