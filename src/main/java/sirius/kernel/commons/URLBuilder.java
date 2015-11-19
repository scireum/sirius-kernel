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
import java.util.Objects;

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
        Objects.nonNull(baseURL);

        url = new StringBuilder();
        if (baseURL.endsWith("/")) {
            url.append(baseURL.substring(0, baseURL.length() - 1));
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
        Objects.nonNull(protocol);
        Objects.nonNull(host);

        url = new StringBuilder();
        url.append(protocol);
        url.append("://");
        url.append(host);
    }

    /**
     * Adds a path part to the url.
     * <p>
     * Once the first parameter has been added, the path can no longer be modified.
     *
     * @param uriPartsToAdd the uri part to add. This should not contain any '/' as these are added automatically. If
     *                      an array (vararg) is given, all components are appended to the internal {@link
     *                      StringBuilder} without any additional characters.
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
        if (questionMark.firstCall()) {
            url.append("?");
        } else {
            url.append("&");
        }
        url.append(key);
        url.append("=");
        url.append(Strings.urlEncode(value == null ? "" : value.toString()));

        return this;
    }

    @Override
    public String toString() {
        return url.toString();
    }
}
