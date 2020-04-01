/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Node;
import sirius.kernel.health.Exceptions;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * Uses an XMLStructuredOutput with a temporary buffer to generate XML into a String.
 */
@ParametersAreNonnullByDefault
public class XMLGenerator extends XMLStructuredOutput {

    /**
     * Creates a new XMLGenerator which uses an internal buffer to store the XML.
     */
    public XMLGenerator() {
        super(new ByteArrayOutputStream());
    }

    /**
     * Returns the generated XML as string using the given encoding.
     *
     * @param encoding the encoding to use when converting the binary buffer to a String.
     * @return a string representation of the generated XML.
     */
    public String generate(String encoding) {
        try {
            return new String(((ByteArrayOutputStream) out).toByteArray(), encoding);
        } catch (UnsupportedEncodingException e) {
            throw Exceptions.handle(e);
        }
    }

    /**
     * Returns the generated XML as string, using UTF-8 as encoding.
     *
     * @return a string representation of the generated XML.
     */
    public String generate() {
        return generate(StandardCharsets.UTF_8.name());
    }

    /**
     * Writes the given XML document to the given writer.
     *
     * @param doc      the XML document to write
     * @param writer   the target to write the XML to
     * @param encoding the encoding used to write the XML
     * @throws javax.xml.transform.TransformerException if an exception during serialization occurs.
     */
    public static void writeXML(Node doc, Writer writer, String encoding) throws TransformerException {
        writeXML(doc, writer, encoding, false);
    }

    /**
     * Writes the given XML document to the given writer.
     *
     * @param doc                the XML document to write
     * @param writer             the target to write the XML to
     * @param encoding           the encoding used to write the XML
     * @param omitXMLDeclaration determines whether the XML declaration should be skipped (<tt>true</tt>) or not
     *                           (<tt>false</tt>).
     * @throws javax.xml.transform.TransformerException if an exception during serialization occurs.
     */
    public static void writeXML(Node doc, Writer writer, String encoding, boolean omitXMLDeclaration)
            throws TransformerException {
        writeXML(doc, writer, encoding, omitXMLDeclaration, true);
    }

    /**
     * Writes the given XML document to the given writer.
     *
     * @param doc                the XML document to write
     * @param writer             the target to write the XML to
     * @param encoding           the encoding used to write the XML
     * @param omitXMLDeclaration determines whether the XML declaration should be skipped (<tt>true</tt>) or not
     *                           (<tt>false</tt>).
     * @param includeOuter       <tt>true</tt> if the outer XML should be included, <tt>false</tt> otherwise
     * @throws TransformerException if an exception during serialization occurs
     */
    public static void writeXML(Node doc,
                                Writer writer,
                                String encoding,
                                boolean omitXMLDeclaration,
                                boolean includeOuter) throws TransformerException {
        StreamResult streamResult = new StreamResult(writer);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer serializer = tf.newTransformer();
        serializer.setOutputProperty(OutputKeys.ENCODING, encoding);
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        if (omitXMLDeclaration) {
            serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        }

        if (includeOuter) {
            DOMSource domSource = new DOMSource(doc);
            serializer.transform(domSource, streamResult);

            return;
        }

        doc = doc.getFirstChild();
        while (doc != null) {
            DOMSource domSource = new DOMSource(doc);
            serializer.transform(domSource, streamResult);
            doc = doc.getNextSibling();
        }
    }

    /**
     * Creates a new xml document.
     *
     * @param namespaceURI  defines the uri of the default namespace used by the resulting document
     * @param qualifiedName returns the name of the root element of the resulting document
     * @param docType       specifies the DocumentType used by the resulting document
     * @return a Document created by the given specifications
     * @throws javax.xml.parsers.ParserConfigurationException if no suitable xml implementation was found.
     */
    public static Document createDocument(@Nullable String namespaceURI,
                                          String qualifiedName,
                                          @Nullable DocumentType docType) throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        DOMImplementation impl = builder.getDOMImplementation();
        return impl.createDocument(namespaceURI, qualifiedName, docType);
    }
}
