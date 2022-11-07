/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import java.io.Serial;
import java.net.URL;

/**
 * Wraps a SOAP fault as <tt>Exception</tt>.
 */
public class SOAPFaultException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1657270622312190730L;

    private final String action;
    private final URL endpoint;
    private final String faultCode;

    /**
     * Creates a new exception which wraps the given SOAP fault.
     *
     * @param action            the action which was attempted
     * @param effectiveEndpoint the effective endpoint which has been addressed
     * @param faultCode         the code of the fault that occurred
     * @param faultMessage      the message of the fault
     */
    public SOAPFaultException(String action, URL effectiveEndpoint, String faultCode, String faultMessage) {
        super(faultMessage);
        this.action = action;
        this.endpoint = effectiveEndpoint;
        this.faultCode = faultCode;
    }

    public String getFaultCode() {
        return faultCode;
    }

    public String getAction() {
        return action;
    }

    public URL getEndpoint() {
        return endpoint;
    }
}
