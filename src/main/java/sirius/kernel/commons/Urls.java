/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

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
