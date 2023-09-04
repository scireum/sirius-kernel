/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

/**
 * Tests the [URLBuilder] class.
 */
class URLBuilderTest {

    @Test
    fun `baseURL is handled correctly`() {
        var urlBuilder = URLBuilder("http://sirius-lib.net")
        assertEquals("http://sirius-lib.net", urlBuilder.build())

        urlBuilder = URLBuilder("http://sirius-lib.net/")
        assertEquals("http://sirius-lib.net", urlBuilder.build())

        urlBuilder = URLBuilder("sirius-lib.net")
        assertEquals("sirius-lib.net", urlBuilder.build())

        urlBuilder = URLBuilder("sirius-lib.net/")
        assertEquals("sirius-lib.net", urlBuilder.build())

        urlBuilder = URLBuilder("www.sirius-lib.net")
        assertEquals("www.sirius-lib.net", urlBuilder.build())

        urlBuilder = URLBuilder("www.sirius-lib.net/")
        assertEquals("www.sirius-lib.net", urlBuilder.build())

        urlBuilder = URLBuilder("www.sirius-lib.net/example")
        assertEquals("www.sirius-lib.net/example", urlBuilder.build())

        urlBuilder = URLBuilder("www.sirius-lib.net/example/")
        assertEquals("www.sirius-lib.net/example", urlBuilder.build())

        urlBuilder = URLBuilder("http://www.sirius-lib.net")
        assertEquals("http://www.sirius-lib.net", urlBuilder.build())

        urlBuilder = URLBuilder("http://www.sirius-lib.net/")
        assertEquals("http://www.sirius-lib.net", urlBuilder.build())

        urlBuilder = URLBuilder("localhost:80")
        assertEquals("localhost:80", urlBuilder.build())

        urlBuilder = URLBuilder("localhost:80/")
        assertEquals("localhost:80", urlBuilder.build())

        urlBuilder = URLBuilder("127.0.0.1:80/")
        assertEquals("127.0.0.1:80", urlBuilder.build())
    }

    @Test
    fun `baseURL creation with protocol and host constructor is working`() {

        var urlBuilder = URLBuilder(URLBuilder.PROTOCOL_HTTP, "sirius-lib.net")
        assertEquals("http://sirius-lib.net", urlBuilder.build())

        urlBuilder = URLBuilder(URLBuilder.PROTOCOL_HTTPS, "sirius-lib.net")
        assertEquals("https://sirius-lib.net", urlBuilder.build())

        urlBuilder = URLBuilder(URLBuilder.PROTOCOL_HTTP, "www.sirius-lib.net")
        assertEquals("http://www.sirius-lib.net", urlBuilder.build())

        urlBuilder = URLBuilder(URLBuilder.PROTOCOL_HTTP, "www.sirius-lib.net/example")
        assertEquals("http://www.sirius-lib.net/example", urlBuilder.build())

        urlBuilder = URLBuilder(URLBuilder.PROTOCOL_HTTPS, "www.sirius-lib.net/example")
        assertEquals("https://www.sirius-lib.net/example", urlBuilder.build())

        urlBuilder = URLBuilder(URLBuilder.PROTOCOL_HTTP, "localhost:80")
        assertEquals("http://localhost:80", urlBuilder.build())

        urlBuilder = URLBuilder(URLBuilder.PROTOCOL_HTTPS, "localhost:80")
        assertEquals("https://localhost:80", urlBuilder.build())

        urlBuilder = URLBuilder(URLBuilder.PROTOCOL_HTTP, "127.0.0.1:80")
        assertEquals("http://127.0.0.1:80", urlBuilder.build())

        urlBuilder = URLBuilder(URLBuilder.PROTOCOL_HTTPS, "127.0.0.1:80")
        assertEquals("https://127.0.0.1:80", urlBuilder.build())
    }

    @Test
    fun `adding a single part is handled correctly`() {
        var urlBuilder = URLBuilder(URLBuilder.PROTOCOL_HTTP, "sirius-lib.net")
        urlBuilder.addSafePart("")
        assertEquals("http://sirius-lib.net", urlBuilder.build())

        urlBuilder = URLBuilder(URLBuilder.PROTOCOL_HTTP, "sirius-lib.net")
        urlBuilder.addSafePart("example")
        assertEquals("http://sirius-lib.net/example", urlBuilder.build())

        urlBuilder = URLBuilder(URLBuilder.PROTOCOL_HTTP, "sirius-lib.net")
        urlBuilder.addSafePart("/example/")
        assertEquals("http://sirius-lib.net/example", urlBuilder.build())

        urlBuilder = URLBuilder(URLBuilder.PROTOCOL_HTTP, "sirius-lib.net")
        urlBuilder.addSafePart("file.jpg")
        assertEquals("http://sirius-lib.net/file.jpg", urlBuilder.build())

        urlBuilder = URLBuilder(URLBuilder.PROTOCOL_HTTP, "sirius-lib.net")
        urlBuilder.addSafePart("example/second/file.jpg")
        assertEquals("http://sirius-lib.net/example/second/file.jpg", urlBuilder.build())

    }

    @Test
    fun `adding multiple parts through varargs is handled correctly`() {
        var urlBuilder = URLBuilder(URLBuilder.PROTOCOL_HTTP, "sirius-lib.net")
        urlBuilder.addSafeParts("", "", "", "")
        assertEquals("http://sirius-lib.net", urlBuilder.build())

        urlBuilder = URLBuilder(URLBuilder.PROTOCOL_HTTP, "sirius-lib.net")
        urlBuilder.addSafeParts("example", "/", "file.jpg")
        assertEquals("http://sirius-lib.net/example/file.jpg", urlBuilder.build())

        urlBuilder = URLBuilder(URLBuilder.PROTOCOL_HTTP, "sirius-lib.net")
        urlBuilder.addSafeParts("very", "long", "example")
        assertEquals("http://sirius-lib.net/very/long/example", urlBuilder.build())

        urlBuilder = URLBuilder(URLBuilder.PROTOCOL_HTTP, "sirius-lib.net")
        urlBuilder.addSafeParts("/very/", "//long//", "/example/")
        assertEquals("http://sirius-lib.net/very/long/example", urlBuilder.build())

        urlBuilder = URLBuilder(URLBuilder.PROTOCOL_HTTP, "sirius-lib.net")
        urlBuilder.addSafeParts("example", "", "/", "", "file.jpg")
        assertEquals("http://sirius-lib.net/example/file.jpg", urlBuilder.build())
    }

    @Test
    fun `the standard method for adding parameters encodes the value`() {
        val urlBuilder = URLBuilder(URLBuilder.PROTOCOL_HTTP, "sirius-lib.net")
        urlBuilder.addParameter("test", "test value")
        assertEquals("http://sirius-lib.net?test=test+value", urlBuilder.build())
    }

    @Test
    fun `a single parameter is added and url encoded correctly`() {
        var urlBuilder = URLBuilder(URLBuilder.PROTOCOL_HTTP, "sirius-lib.net")
        urlBuilder.addParameter("test", "", true)
        assertEquals("http://sirius-lib.net?test=", urlBuilder.build())

        urlBuilder = URLBuilder(URLBuilder.PROTOCOL_HTTP, "sirius-lib.net")
        urlBuilder.addParameter("test", "", false)
        assertEquals("http://sirius-lib.net?test=", urlBuilder.build())

        urlBuilder = URLBuilder(URLBuilder.PROTOCOL_HTTP, "sirius-lib.net")
        urlBuilder.addParameter("test", "value", true)
        assertEquals("http://sirius-lib.net?test=value", urlBuilder.build())

        urlBuilder = URLBuilder(URLBuilder.PROTOCOL_HTTP, "sirius-lib.net")
        urlBuilder.addParameter("test", "value", false)
        assertEquals("http://sirius-lib.net?test=value", urlBuilder.build())

        urlBuilder = URLBuilder(URLBuilder.PROTOCOL_HTTP, "sirius-lib.net")
        urlBuilder.addParameter("test", "test value", true)
        assertEquals("http://sirius-lib.net?test=test+value", urlBuilder.build())

        urlBuilder = URLBuilder(URLBuilder.PROTOCOL_HTTP, "sirius-lib.net")
        urlBuilder.addParameter("test", "test+value", false)
        assertEquals("http://sirius-lib.net?test=test+value", urlBuilder.build())

        urlBuilder = URLBuilder(URLBuilder.PROTOCOL_HTTP, "sirius-lib.net")
        urlBuilder.addParameter("test", "#value", true)
        assertEquals("http://sirius-lib.net?test=%23value", urlBuilder.build())

        urlBuilder = URLBuilder(URLBuilder.PROTOCOL_HTTP, "sirius-lib.net")
        urlBuilder.addParameter("test", "%23value", false)
        assertEquals("http://sirius-lib.net?test=%23value", urlBuilder.build())
    }

    @Test
    fun `multiple parameters are added correctly`() {
        val urlBuilder = URLBuilder(URLBuilder.PROTOCOL_HTTP, "sirius-lib.net")

        urlBuilder.addParameter("test1", "value1")
        urlBuilder.addParameter("test2", "value2")
        urlBuilder.addParameter("test3", "value3")
        assertEquals("http://sirius-lib.net?test1=value1&test2=value2&test3=value3", urlBuilder.build())

    }

    @Test
    fun `can't add parts after a parameter has been added`() {
        assertThrows<IllegalStateException> {
            val urlBuilder = URLBuilder(URLBuilder.PROTOCOL_HTTP, "sirius-lib.net")
            urlBuilder.addParameter("test", "value")
            urlBuilder.addSafePart("late")
        }

    }

    @Test
    fun `baseurl with query part can be extended with another parameter`() {
        val urlBuilder = URLBuilder("http://sirius-lib.net?already=there")
        urlBuilder.addParameter("test", "test value")
        assertEquals("http://sirius-lib.net?already=there&test=test+value", urlBuilder.build())
    }
}
