/*
 * Made with all the love in the world
 * by scireum in Stuttgart, Germany
 *
 * Copyright by scireum GmbH
 * https://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
    fun encodePathSegment() {
        assertEquals("hello%20world", Urls.encodePathSegment("hello world"))
        assertEquals("hello%2Bworld", Urls.encodePathSegment("hello+world"))
        assertEquals("hello%2Fworld", Urls.encodePathSegment("hello/world"))
        assertEquals("A%3FTEST%26B%C3%84%C3%96%C3%9C", Urls.encodePathSegment("A?TEST&BÄÖÜ"))
        assertEquals("", Urls.encodePathSegment(""))
        assertNull(Urls.encodePathSegment(null))
    }

    @ParameterizedTest
    @ValueSource(strings = ["hello world", "hello+world", "hello%20world", "hello/world", "A?TEST&BÄÖÜ #1"])
    fun `encodePathSegment survives both path and form decoding`(value: String) {
        val encoded = Urls.encodePathSegment(value)
        assertEquals("/$value", URI.create("https://example.com/$encoded").path)
        assertEquals(value, Urls.decode(encoded))
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
