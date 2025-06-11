/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests the [Strings] class.
 */
class UrlsTest {

    @Test
    fun quoteSpaces() {
        assertEquals("https://example.com/hello%20world", Urls.quoteSpaces("https://example.com/hello world"))
        assertEquals("https://example.com/hello%20world", Urls.quoteSpaces("https://example.com/hello%20world"))
        assertEquals("https://example.com/hello+world", Urls.quoteSpaces("https://example.com/hello+world"))
        assertEquals("https://example.com/helloworld?test=a%20b", Urls.quoteSpaces("https://example.com/helloworld?test=a b"))
        assertEquals("https://example.com/helloworld?test=a%20b", Urls.quoteSpaces("https://example.com/helloworld?test=a%20b"))
        assertEquals("https://example.com/helloworld?test=a+b", Urls.quoteSpaces("https://example.com/helloworld?test=a+b"))
    }
}
