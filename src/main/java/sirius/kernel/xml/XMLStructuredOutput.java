/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import sirius.kernel.health.Exceptions;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Represents a {@link StructuredOutput} emitting XML data.
 * <p>
 * Can be used to construct XML using the <tt>StructuredOutput</tt> interface.
 */
public class XMLStructuredOutput extends AbstractStructuredOutput {

    private TransformerHandler transformerHandler;
    protected OutputStream out;
    private int opensCalled = 0;

    /**
     * Creates a new output writing to the given output stream.
     *
     * @param out the stream used as destination for the generated xml
     */
    public XMLStructuredOutput(@Nonnull OutputStream out) {
        this(out, null);
    }

    /**
     * Creates a new output writing to the given output stream.
     *
     * @param output  the stream used as destination for the generated xml
     * @param doctype the doc type used in the XML header
     */
    public XMLStructuredOutput(@Nonnull OutputStream output, @Nullable String doctype) {
        this(output, StandardCharsets.UTF_8, doctype);
    }

    /**
     * Creates a new output writing to the given output stream.
     *
     * @param output   the stream used as destination for the generated xml
     * @param encoding the charset used to encode the output
     * @param doctype  the doc type used in the XML header
     */
    public XMLStructuredOutput(@Nonnull OutputStream output, @Nonnull Charset encoding, @Nullable String doctype) {
        try {
            this.out = output;
            StreamResult streamResult = new StreamResult(out);
            SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory.newInstance();
            transformerHandler = tf.newTransformerHandler();
            Transformer serializer = transformerHandler.getTransformer();
            if (doctype != null) {
                serializer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype);
            }
            serializer.setOutputProperty(OutputKeys.ENCODING, encoding.name());
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformerHandler.setResult(streamResult);
            transformerHandler.startDocument();
        } catch (Exception e) {
            throw Exceptions.handle(e);
        }
    }

    @Override
    protected void endArray(String name) {
        try {
            transformerHandler.endElement("", "", name);
        } catch (SAXException e) {
            throw Exceptions.handle(e);
        }
    }

    @Override
    protected void endObject(String name) {
        try {
            transformerHandler.endElement("", "", name);
        } catch (SAXException e) {
            throw Exceptions.handle(e);
        }
    }

    @Override
    public StructuredOutput beginResult() {
        return beginOutput("result");
    }

    @Override
    public StructuredOutput beginResult(String name) {
        return beginOutput(name);
    }

    /**
     * Starts the output with the given root element.
     *
     * @param rootElement the name of the root element of the generated document.
     * @return the output itself for fluent method calls
     */
    public StructuredOutput beginOutput(@Nonnull String rootElement) {
        if (opensCalled == 0) {
            try {
                transformerHandler.startDocument();
            } catch (SAXException e) {
                throw Exceptions.handle(e);
            }
        }
        opensCalled++;
        beginObject(rootElement);

        return this;
    }

    /**
     * Starts the output with the given root element and attributes
     *
     * @param rootElement the name of the root element of the generated document.
     * @param attr        the attributes for the root element
     * @return the output itself for fluent method calls
     */
    public StructuredOutput beginOutput(@Nonnull String rootElement, Attribute... attr) {
        if (opensCalled == 0) {
            try {
                transformerHandler.startDocument();
            } catch (SAXException e) {
                throw Exceptions.handle(e);
            }
        }
        opensCalled++;
        beginObject(rootElement, attr);

        return this;
    }

    /**
     * Convenience method for {@link #beginOutput(String, Attribute...)} prepending a namespace.
     *
     * @param namespace   the namespace
     * @param rootElement the name of the root element of the generated document
     * @param attr        the attributes for the root element
     * @return the output itself for fluent method calls
     */
    public StructuredOutput beginOutput(@Nonnull String namespace, @Nonnull String rootElement, Attribute... attr) {
        return beginOutput(namespace + ":" + rootElement, attr);
    }

    /**
     * Creates a {@link AbstractStructuredOutput.TagBuilder} used to fluently create the root element.
     *
     * @param rootElement name of the root element
     * @return a tag builder which can be used to build the root element
     */
    @CheckReturnValue
    public TagBuilder buildBegin(@Nonnull String rootElement) {
        if (opensCalled == 0) {
            try {
                transformerHandler.startDocument();
            } catch (SAXException e) {
                throw Exceptions.handle(e);
            }
        }
        opensCalled++;
        return buildObject(rootElement);
    }

    /**
     * Closes the output and this XML document.
     */
    public void endOutput() {
        endObject();
        if (opensCalled-- == 1) {
            super.endResult();
            try {
                transformerHandler.endDocument();
                out.close();
            } catch (SAXException | IOException e) {
                throw Exceptions.handle(e);
            }
        }
    }

    @Override
    public void endResult() {
        endOutput();
    }

    @Override
    protected void startArray(String name) {
        try {
            transformerHandler.startElement("", "", name, null);
        } catch (SAXException e) {
            throw Exceptions.handle(e);
        }
    }

    @Override
    protected void startObject(String name, Attribute... attributes) {
        try {
            AttributesImpl attrs = null;
            if (attributes != null) {
                attrs = new AttributesImpl();
                for (Attribute attr : attributes) {
                    attrs.addAttribute("", "", attr.getName(), "CDATA", String.valueOf(attr.getValue()));
                }
            }
            transformerHandler.startElement("", "", name, attrs);
        } catch (SAXException e) {
            throw Exceptions.handle(e);
        }
    }

    @Override
    protected void writeProperty(String name, Object value) {
        try {
            transformerHandler.startElement("", "", name, null);
            if (value != null) {
                String val = value.toString();
                transformerHandler.characters(val.toCharArray(), 0, val.length());
            }
            transformerHandler.endElement("", "", name);
        } catch (SAXException e) {
            throw Exceptions.handle(e);
        }
    }

    /**
     * Adds a property to the current object.
     * <p>
     * This will create a property only if the specified data object is not null.
     * Else no property is created.
     *
     * @param name the name of the property
     * @param data the value of the property
     * @return the output itself for fluent method calls
     */
    public StructuredOutput propertyIfFilled(@Nonnull String name, @Nullable Object data) {
        if (data != null) {
            property(name, data);
        }
        return this;
    }

    /**
     * Adds a property containing attributes to the current object.
     *
     * @param name       the name of the property
     * @param data       the value of the property
     * @param attributes the attributes for the element
     * @return the output itself for fluent method calls
     */
    public StructuredOutput property(String name, Object data, Attribute... attributes) {
        startObject(name, attributes);
        text(data);
        endObject(name);
        return this;
    }

    /**
     * Adds a property containing attributes to the current object.
     * <p>
     * This will create a property only if the specified data object is not null.
     * Else no property is created.
     *
     * @param name       the name of the property
     * @param data       the value of the property
     * @param attributes the attributes for the element
     * @return the output itself for fluent method calls
     */
    public StructuredOutput propertyIfFilled(String name, Object data, Attribute... attributes) {
        if (data != null) {
            property(name, data, attributes);
        }
        return this;
    }

    /**
     * Adds a property containing attributes to the current object.
     * <p>
     * This will create a property with the specified data as value or empty string if the value is null.
     *
     * @param name       the name of the property
     * @param data       the value of the property
     * @param attributes the attributes for the element
     * @return the output itself for fluent method calls
     */
    public StructuredOutput nullsafeProperty(String name, Object data, Attribute... attributes) {
        property(name, data != null ? data : "", attributes);
        return this;
    }

    /**
     * Creates a text node for the current node.
     *
     * @param text the text to be added to the current node
     * @return the output itself for fluent method calls
     */
    public StructuredOutput text(Object text) {
        try {
            if (text != null) {
                String val = text.toString();
                transformerHandler.characters(val.toCharArray(), 0, val.length());
            }
        } catch (SAXException e) {
            throw Exceptions.handle(e);
        }

        return this;
    }

    /**
     * Closes the underlying stream
     *
     * @throws IOException if an IO error occurs while closing the stream
     */
    public void close() throws IOException {
        out.close();
    }
}
