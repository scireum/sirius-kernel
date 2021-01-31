/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Provides a simple trust manager which trusts self signed certificates.
 */
class TrustingSelfSignedTrustManager implements X509TrustManager {

    private static final X509Certificate[] EMPTY_CERTIFICATE_ARRAY = new X509Certificate[0];

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        throw new CertificateException("This trust manager cannot be used in a server");
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (chain.length != 1) {
            throw new CertificateException("The certificate is not self-signed");
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return EMPTY_CERTIFICATE_ARRAY;
    }
}
