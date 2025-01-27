/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.xml.sax.SAXParseException
import sirius.kernel.SiriusExtension
import sirius.kernel.commons.ValueHolder
import sirius.kernel.health.Counter
import java.io.ByteArrayInputStream
import java.io.IOException
import java.text.ParseException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the [XMLStructuredInput] class.
 */
internal class XMLStructuredInputTest {

    @Test
    fun `Read xml content with external DTD works`() {
        val input = XMLStructuredInput(
            ByteArrayInputStream(//language=xml
                """<?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE something_details SYSTEM "https://something.com/schemas/something/1.0.0/something.dtd">
                <something_details><something_number>123456</something_number></something_details>
                """.toByteArray()
            ), null
        )
        assertEquals("123456", input.root().queryString("."))
    }

    @Test
    fun `Preventing access to local resources works`() {
        assertThrows<IOException> {
            XMLStructuredInput(
                ByteArrayInputStream(//language=xml
                    """<?xml version="1.0" encoding="UTF-8"?>
                    <!DOCTYPE root [<!ENTITY xxe SYSTEM "file:///etc/hosts">]>
                    <root>&xxe;</root>
                """.toByteArray()
                ), null
            )
        }
    }

}
