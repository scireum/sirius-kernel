/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import sirius.kernel.async.CallContext;
import sirius.kernel.async.TaskContext;
import sirius.kernel.health.Exceptions;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A combination of DOM and SAX parser which permits to parse very large XML files while conveniently handling sub tree
 * using a DOM and xpath api.
 * <p>
 * Used SAX to parse a given XML file. A set of {@link NodeHandler} objects can be given, which get notified if
 * a sub-tree below a given tag was parsed. This sub-tree is available as DOM and can conveniently be processed
 * using xpath.
 */
public class XMLReader extends DefaultHandler {

    private TaskContext taskContext;

    private Map<String, NodeHandler> handlers = Maps.newTreeMap();
    private Map<String, Runnable> missingHandlers = Maps.newTreeMap();
    private List<SAX2DOMHandler> activeHandlers = Lists.newArrayList();
    private DocumentBuilder documentBuilder;

    /**
     * Creates a new XMLReader.
     * <p>
     * Use {@link #addHandler(String, NodeHandler)} tobind handlers to tags and then call one of the <tt>parse</tt>
     * methods to process the XML file.
     * <p>
     * To interrupt processing use {@link TaskContext#cancel()}.
     */
    public XMLReader() {
        try {
            documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            taskContext = CallContext.getCurrent().get(TaskContext.class);
        } catch (ParserConfigurationException e) {
            throw Exceptions.handle(e);
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
        // Remove MissingHandler
        missingHandlers.remove(name);
        // Start a new handler is necessary
        NodeHandler handler = handlers.get(name);
        if (handler != null) {
            SAX2DOMHandler saxHandler = new SAX2DOMHandler(handler, documentBuilder.newDocument());
            saxHandler.createElement(name, attributes);
            activeHandlers.add(saxHandler);
        }
        // Check if the user tried to interrupt parsing....
        if (!taskContext.isActive()) {
            throw new UserInterruptException();
        }
    }

    /**
     * Registers a new handler for a qualified name of a node.
     * <p>
     * Handlers are invoked after the complete node was read. Namespaces are ignored for now which eases
     * the processing a lot (especially for xpath related tasks). Namespaces however
     * could be easily added by replacing String with QName here.
     *
     * @param name    the qualified name of the tag which should be parsed and processed
     * @param handler the NodeHandler used to process the parsed DOM sub-tree
     */
    public void addHandler(String name, NodeHandler handler) {
        handlers.put(name, handler);
    }

    /**
     * Registers a Runnable for a qualified name of a node that is ONLY invoked if no node of this name is found
     * in the XML and the reader completes.
     *
     * @param name           the qualified name of the tag which should be looked for.
     * @param missingHandler the Runnable to run if no node of this name was found.
     */
    public void addMissingHandler(String name, Runnable missingHandler) {
        missingHandlers.put(name, missingHandler);
    }

    /**
     * Registers a new handler for a qualified name of a node and registers a Runnable to run if that node is never encountered.
     * <p>
     * See {@link #addHandler(String, NodeHandler)} and {@link #addMissingHandler(String, Runnable)}
     *
     * @param name           the qualified name of the tag which should be parsed and processed
     * @param handler        the NodeHandler used to process the parsed DOM sub-tree
     * @param missingHandler the Runnable to run if no node of this name was found.
     */
    public void addHandler(String name, NodeHandler handler, Runnable missingHandler) {
        addHandler(name, handler);
        addMissingHandler(name, missingHandler);
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
     * Used to handle the an abort via {@link TaskContext}
     */
    static class UserInterruptException extends RuntimeException {

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
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
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
            //run all missingHandlers that remain
            missingHandlers.forEach((name, runnable) -> runnable.run());
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException(e);
        } catch (UserInterruptException e) {
            // IGNORED - this is used to cancel parsing if the used tried to
            // cancel a process.
        } finally {
            stream.close();
        }
    }

    private InputSource tryResolveEntity(String systemId, Function<String, InputStream> resourceLocator)
            throws IOException {
        URL url = new URL(systemId);
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

    public InputSource emptyResource() {
        return new InputSource(new StringReader(""));
    }
}
