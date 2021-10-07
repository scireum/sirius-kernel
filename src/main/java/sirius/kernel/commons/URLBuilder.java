/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Used to succesively build URLs.
 * <p>
 * The major advantage is to be able to add parameters without caring if a '?' or a '&amp;' has to be used.
 */
public class URLBuilder {

    private final StringBuilder url;
    private final Monoflop questionMark = Monoflop.create();

    /**
     * Can be used to specify the HTTP protocol in {@link #URLBuilder(String, String)}.
     */
    public static final String PROTOCOL_HTTP = "http";

    /**
     * Can be used to specify the HTTPS protocol in {@link #URLBuilder(String, String)}.
     */
    public static final String PROTOCOL_HTTPS = "https";

    /**
     * Creates a new instance pre filled with the given baseURL.
     *
     * @param baseURL the base url to stat with in a form like <tt>http://somehost.com</tt>
     */
    public URLBuilder(@Nonnull String baseURL) {
        url = new StringBuilder();
        if (baseURL.endsWith("/")) {
            url.append(baseURL, 0, baseURL.length() - 1);
        } else {
            url.append(baseURL);
        }
    }

    /**
     * Creates a new instance targeting the given host using the given protocol.
     *
     * @param protocol the protocol (http, https) to use
     * @param host     the host to target.
     */
    public URLBuilder(@Nonnull String protocol, @Nonnull String host) {
        url = new StringBuilder();
        url.append(protocol);
        url.append("://");
        url.append(host);
    }

    /**
     * Adds a path part to the url.
     * <p>
     * Once the first parameter has been added, the path can no longer be modified. Also the part itself can (but
     * shouldn't) contain parameters, usually led by an initial question mark.
     *
     * @param uriPartsToAdd the uri part to add. This should not contain a leading '/' as it is added automatically. If
     *                      an array (vararg) is given, all components are appended to the internal {@link
     *                      StringBuilder} without any additional characters. If this contains a '?' to add parameters,
     *                      no more parts can be added.
     * @return the builder itself for fluent method calls
     */
    public URLBuilder addPart(@Nonnull String... uriPartsToAdd) {
        url.append("/");
        for (String uriPart : uriPartsToAdd) {
            if (Strings.isFilled(uriPart)) {
                if (questionMark.isToggled()) {
                    throw new IllegalStateException(Strings.apply(
                            "Cannot add '%s'! Parameters where already added to: '%s'.",
                            uriPart,
                            url));
                }
                if (uriPart.contains("?")) {
                    questionMark.toggle();
                }
                url.append(uriPart);
            }
        }

        return this;
    }

    /**
     * Adds a parameter to the url.
     *
     * @param key   the name of the parameter
     * @param value the value of the parameter which will be url encoded. If the given value is <tt>null</tt> an empty
     *              parameter will be added.
     * @return the builder itself for fluent method calls
     */
    public URLBuilder addParameter(@Nonnull String key, @Nullable Object value) {
        return addParameter(key, value, true);
    }

    /**
     * Adds a parameter to the url.
     *
     * @param key       the name of the parameter
     * @param value     the value of the parameter . If the given value is <tt>null</tt> an empty parameter will be added.
     * @param urlEncode <tt>true</tt> if the given value should be url encoded before adding
     * @return the builder itself for fluent method calls
     */
    public URLBuilder addParameter(@Nonnull String key, @Nullable Object value, boolean urlEncode) {
        if (questionMark.firstCall()) {
            url.append("?");
        } else {
            url.append("&");
        }
        url.append(key);
        url.append("=");
        String stringValue = value == null ? "" : value.toString();
        if (urlEncode) {
            stringValue = Strings.urlEncode(stringValue);
        }
        url.append(stringValue);

        return this;
    }

    @Override
    public String toString() {
        return url.toString();
    }

    /**
     * Builds the url and returns it as a string.
     *
     * @return the url that was built as string
     */
    public String build() {
        return url.toString();
    }

    /**
     * Creates a {@link URL URL object} from the resulting string of this builder.
     * <p>
     * Only works if the url contains a valid protocol.
     *
     * @return the url that was built as a {@link URL URL object}
     * @throws IllegalStateException should only happen if no protocol or an invalid protocol has been given
     */
    public URL asURL() {
        try {
            return new URL(url.toString());
        } catch (MalformedURLException e) {
            throw new IllegalStateException(Strings.apply("Could not create URL: %s", e.getMessage()));
        }
    }

    /**
     * Creates a {@link URI URI object} from the resulting string of this builder.
     * <p>
     * Only works if the url contains a valid protocol.
     * This internally uses {@link #asURL()} as we only want valid URLs to be build as URI.
     *
     * @return the url that was built as a {@link URI URI object}
     * @throws IllegalStateException should only happen if no protocol or an invalid protocol has been given
     */
    public URI asURI() {
        try {
            return asURL().toURI();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(Strings.apply("Could not create URI: %s", e.getMessage()));
        }
    }
}
