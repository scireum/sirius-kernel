/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;

/**
 * Provides various helper methods for working with {@linkplain java.net.URI URIs} and {@linkplain java.net.URL URLs}.
 * <p>
 * This class can and should not be instantiated, as all methods are static.
 */
public class Urls {

    private Urls() {
        // prevent instantiation
    }

    /**
     * Returns if the given string is an HTTP(S) URL.
     *
     * @param value the string to check
     * @return <tt>true</tt> if the given string is an HTTP(S) URL, <tt>false</tt> otherwise
     */
    public static boolean isHttpUrl(@Nullable String value) {
        return isUrl(value,
                     url -> "http".equalsIgnoreCase(url.getProtocol()) || "https".equalsIgnoreCase(url.getProtocol()));
    }

    /**
     * Returns if the given string is an HTTPS URL, explicitly excluding unencrypted HTTP URLs.
     *
     * @param value the string to check
     * @return <tt>true</tt> if the given string is an HTTPS URL, <tt>false</tt> otherwise
     */
    public static boolean isHttpsUrl(@Nullable String value) {
        return isUrl(value, url -> "https".equalsIgnoreCase(url.getProtocol()));
    }

    private static boolean isUrl(@Nullable String value, Predicate<URL> checker) {
        if (Strings.isEmpty(value)) {
            return false;
        }

        try {
            return checker.test(URI.create(value).toURL());
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * Returns a URL-encoded representation of the given <tt>value</tt> with <tt>UTF-8</tt> as character encoding.
     *
     * @param value the value to be encoded.
     * @return a URL-encoded representation of value, using UTF-8 as character encoding.
     */
    @Nullable
    public static String encode(@Nullable String value) {
        if (Strings.isFilled(value)) {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        }
        return value;
    }

    /**
     * Returns a URL-decoded representation of the given <tt>value</tt> with <tt>UTF-8</tt> as character encoding.
     *
     * @param value the value to be decoded.
     * @return a URL-decoded representation of value, using UTF-8 as character encoding.
     */
    @Nullable
    public static String decode(@Nullable String value) {
        if (Strings.isFilled(value)) {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        }
        return value;
    }

    /**
     * Tries to fix the given URL by replacing spaces with "%20".
     *
     * @param url the URL to fix
     * @return the fixed URL with spaces replaced by "%20", or the original URL if it was null or empty
     */
    public static String quoteSpaces(String url) {
        if (Strings.isEmpty(url)) {
            return url;
        }
        return url.replace(" ", "%20");
    }
}
