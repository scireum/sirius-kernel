/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import sirius.kernel.commons.Strings;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;
import sirius.kernel.nls.Formatter;

import javax.xml.namespace.NamespaceContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Simple call to send XML to a server (URL) and receive XML back.
 */
public class XMLCall {

    protected Outcall outcall;
    private NamespaceContext namespaceContext;
    private Log debugLogger = Log.get("xml");

    /**
     * Creates a new XMLCall for the given url with Content-Type 'text/xml'.
     *
     * @param url the target URL to call
     * @return an <tt>XMLCall</tt> which can be used to send and receive XML
     * @throws IOException in case of an IO error
     */
    public static XMLCall to(URL url) throws IOException {
        try {
            return to(url.toURI());
        } catch (URISyntaxException e) {
            throw Exceptions.handle(e);
        }
    }

    /**
     * Creates a new XMLCall for the given url.
     *
     * @param url         the target URL to call
     * @param contentType the Content-Type to use
     * @return a new instance to perform the xml call
     * @throws IOException in case of an IO error
     */
    public static XMLCall to(URL url, String contentType) throws IOException {
        try {
            return to(url.toURI(), contentType);
        } catch (URISyntaxException e) {
            throw Exceptions.handle(e);
        }
    }

    /**
     * Creates a new XMLCall for the given uri with Content-Type 'text/xml'.
     *
     * @param uri the target URI to call
     * @return an <tt>XMLCall</tt> which can be used to send and receive XML
     * @throws IOException in case of an IO error
     */
    public static XMLCall to(URI uri) throws IOException {
        return to(uri, "text/xml");
    }

    /**
     * Creates a new XMLCall for the given uri.
     *
     * @param uri         the target URI to call
     * @param contentType the Content-Type to use
     * @return a new instance to perform the xml call
     * @throws IOException in case of an IO error
     */
    public static XMLCall to(URI uri, String contentType) throws IOException {
        XMLCall result = new XMLCall();
        result.outcall = new Outcall(uri);
        result.outcall.setRequestProperty("Content-Type", contentType);
        return result;
    }

    /**
     * Logs the request and the resulting response to the given {@code logger} using the <tt>FINE</tt> level.
     * <p>
     * The default logger is "xml".
     *
     * @param logger the logger to log to
     * @return the XML call itself for fluent method calls
     */
    public XMLCall withFineLogger(Log logger) {
        this.debugLogger = logger;
        return this;
    }

    /**
     * Adds a custom header field to the call
     *
     * @param name  name of the field
     * @param value value of the field
     * @return returns the XML call itself for fluent method calls
     */
    public XMLCall addHeader(String name, String value) {
        outcall.setRequestProperty(name, value);
        return this;
    }

    /**
     * Specifies the <tt>NamespaceContext</tt> to use for the result of this call.
     * <p>
     * This is required to properly execute XPATH expressions on namespaced XMLs.
     *
     * @param namespaceContext the namespace context to use
     * @return the call itself for fluent method calls
     */
    public XMLCall withNamespaceContext(NamespaceContext namespaceContext) {
        this.namespaceContext = namespaceContext;
        return this;
    }

    /**
     * Returns the underlying <tt>Outcall</tt>.
     *
     * @return the underlying outcall
     */
    public Outcall getOutcall() {
        return outcall;
    }

    /**
     * Can be used to generate the XML request.
     * <p>
     * This will mark the underlying {@link Outcall} as a POST request.
     *
     * @return the output which can be used to generate an XML document which is sent to the URL
     * @throws IOException in case of an IO error while sending the XML document
     */
    public XMLStructuredOutput getOutput() throws IOException {
        return new XMLStructuredOutput(outcall.postFromOutput());
    }

    protected InputStream getInputStream() throws IOException {
        if (debugLogger != null && debugLogger.isFINE()) {
            // log the request, even when parsing fails
            try (InputStream body = outcall.getResponse().body()) {
                byte[] bytes = body.readAllBytes();
                logRequest(new String(bytes, outcall.getContentEncoding()));
                return new ByteArrayInputStream(bytes);
            }
        }
        return outcall.getResponse().body();
    }

    protected void logRequest(String response) throws IOException {
        debugLogger.FINE(Formatter.create("""
                                                  ---------- call ----------
                                                  ${httpMethod} ${url} [
                                                                               
                                                  ${callBody}]
                                                  ---------- response ----------
                                                  HTTP-Response-Code: ${responseCode}
                                                                               
                                                  ${response}
                                                  ---------- end ----------
                                                  """)
                                  .set("httpMethod", outcall.getRequest().method())
                                  .set("url", outcall.getRequest().uri())
                                  .set("callBody",
                                       outcall.getRequest().bodyPublisher().isPresent() ? getOutput() : null)
                                  .set("responseCode", getOutcall().getResponseCode())
                                  .set("response", response)
                                  .smartFormat());
    }

    /**
     * Provides access to the XML answer of the call.
     *
     * @return the XML result of the call
     * @throws IOException in case of an IO error while receiving the result
     */
    public XMLStructuredInput getInput() throws IOException {
        // call #getInputStream() before checking for errors, as #getInputStream may log the request/response
        try (InputStream body = getInputStream()) {
            String contentType = outcall.getHeaderField("content-type");
            if (!outcall.isErroneous() || (contentType != null && contentType.toLowerCase().contains("xml"))) {
                return new XMLStructuredInput(body, namespaceContext);
            }
            throw new IOException(Strings.apply("A non-OK response (%s) was received as a result of an HTTP call",
                                                outcall.getResponse().statusCode()));
        }
    }
}
