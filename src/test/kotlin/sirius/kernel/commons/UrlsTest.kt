/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals

/**
 * Tests the [Urls] class.
 */
class UrlsTest {

    @ParameterizedTest
    @CsvSource(
        "true, https://example.com",
        "true, HTTPS://example.com",
        "true, http://example.com",
        "true, Http://example.com?foo=bar",
        "true, http://user:password@server.com/path",
        "true, http://user@server.com/path",
        "true, https://example.com/my/sample/page",
        "true, http://example.com:8080/my/sample/page?user=foo&password=bar",
        "false, https:// ;%@@ lol whatever i don't care",
        "false, HttpS",
        "false, ",
        "false, ''",
        "false, For testing look at https://example.com"
    )
    fun isHttpUrl(isUrl: Boolean, url: String?) {
        assertEquals(isUrl, Urls.isHttpUrl(url))
    }

    @ParameterizedTest
    @CsvSource(
        "true, https://example.com",
        "true, HTTPS://example.com",
        "false, http://example.com",
        "false, Http://example.com?foo=bar",
        "false, http://user:password@server.com/path",
        "false, http://user@server.com/path",
        "true, https://example.com/my/sample/page",
        "false, http://example.com:8080/my/sample/page?user=foo&password=bar",
        "false, https:// ;%@@ lol whatever i don't care",
        "false, HttpS",
        "false, ",
        "false, ''",
        "false, For testing look at https://example.com"
    )
    fun isHttpsUrl(isUrl: Boolean, url: String?) {
        assertEquals(isUrl, Urls.isHttpsUrl(url))
    }

    @Test
    fun encode() {
        assertEquals("A%3FTEST%26B%C3%84%C3%96%C3%9C", Urls.encode("A?TEST&BÄÖÜ"))
    }

    @Test
    fun decode() {
        assertEquals("A?TEST&BÄÖÜ", Urls.decode("A%3FTEST%26B%C3%84%C3%96%C3%9C"))
    }

    @Test
    fun quoteSpaces() {
        assertEquals("https://example.com/hello%20world", Urls.quoteSpaces("https://example.com/hello world"))
        assertEquals("https://example.com/hello%20world", Urls.quoteSpaces("https://example.com/hello%20world"))
        assertEquals("https://example.com/hello+world", Urls.quoteSpaces("https://example.com/hello+world"))
        assertEquals(
            "https://example.com/helloworld?test=a%20b", Urls.quoteSpaces("https://example.com/helloworld?test=a b")
        )
        assertEquals(
            "https://example.com/helloworld?test=a%20b", Urls.quoteSpaces("https://example.com/helloworld?test=a%20b")
        )
        assertEquals(
            "https://example.com/helloworld?test=a+b", Urls.quoteSpaces("https://example.com/helloworld?test=a+b")
        )
    }
}
