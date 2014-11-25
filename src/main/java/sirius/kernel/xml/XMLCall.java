/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import java.io.IOException;
import java.net.URL;

/**
 * Simple call to send XML to a server (URL) and receive XML back.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public class XMLCall {

    private Outcall outcall;

    /**
     * Creates a new XMLCall for the given url with Content-Type 'text/xml'.
     *
     * @param url the target URL to call
     * @return an <tt>XMLCall</tt> which can be used to send and receive XML
     * @throws IOException in case of an IO error
     */
    public static XMLCall to(URL url) throws IOException {
        return to(url, "text/xml");
    }

    /**
     * Creates a new XMLCall for the given url.
     *
     * @param url the target URL to call
     * @param contentType the Content-Type to use
     * @throws IOException in case of an IO error
     */
    public static XMLCall to(URL url, String contentType) throws IOException {
        XMLCall result = new XMLCall();
        result.outcall = new Outcall(url);
        result.outcall.setRequestProperty("Content-Type", contentType);
        return result;
    }

    /**
     * Adds a custom headerfield to the call
     * @param name name of the field
     * @param value value of the field
     */
    public void addHeader(String name, String value){
        outcall.setRequestProperty(name, value);
    }

    /**
     * Provides access to the XML answer of the call.
     *
     * @return the XML result of the call
     * @throws IOException in case of an IO error while receiving the result
     */
    public XMLStructuredOutput getOutput() throws IOException {
        return new XMLStructuredOutput(outcall.getOutput());
    }

    /**
     * Can be used to generate the XML request.
     *
     * @return the an input which can be used to generate an XML document which is sent to the URL
     * @throws IOException in case of an IO error while sending the XML document
     */
    public XMLStructuredInput getInput() throws IOException {
        return new XMLStructuredInput(outcall.getInput(), true);
    }
}
