/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import java.net.URL;

public class SOAPFaultException extends RuntimeException {

    private String action;
    private URL endpoint;
    private String faultCode;

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
