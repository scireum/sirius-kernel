/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import sirius.kernel.SiriusExtension
import sirius.kernel.commons.ValueHolder
import sirius.kernel.health.Counter
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the [XMLReader] class.
 */
@ExtendWith(SiriusExtension::class)
internal class XmlReaderTest {
    @Test
    fun `XMLReader extracts XPATH expression`() {
        val readString = ValueHolder.of<String?>(null)
        val nodeCount = Counter()
        val reader = XMLReader()
        reader.addHandler("test") { node: StructuredNode ->
            nodeCount.inc()
            readString.set(node.queryString("value"))
        }

        reader.parse(
                ByteArrayInputStream(//language=xml
                        """
                            <doc>
                                <test>
                                    <value>1</value>
                                </test>
                                <test>
                                    <value>2</value>
                                </test>
                                <test>
                                    <value>5</value>
                                </test>
                            </doc>
                        """.trimIndent().toByteArray()
                )
        )

        assertEquals("5", readString.get())
        assertEquals(3, nodeCount.count, "parsed invalid count of nodes")
    }

    @Test
    fun `XMLReader supports compound XPATH paths`() {
        val shouldToggle = ValueHolder.of(false)
        val shouldNotToggle = ValueHolder.of(false)
        val reader = XMLReader()
        reader.addHandler("doc/test/value") { _: StructuredNode? ->
            shouldToggle.set(
                    true
            )
        }
        reader.addHandler("value") { _: StructuredNode? -> shouldNotToggle.set(true) }

        reader.parse(
                ByteArrayInputStream(//language=xml
                        """
                            <doc>
                                <test>
                                    <value>1</value>
                                </test>
                                <test>
                                    <value>2</value>
                                </test>
                                <test>
                                    <value>5</value>
                                </test>
                            </doc>
                        """.trimIndent().toByteArray()
                )
        )

        assertTrue { shouldToggle.get() }
        assertFalse { shouldNotToggle.get() }
    }

    @Test
    fun `XMLReader reads attributes`() {
        val attributes: MutableMap<String, String> = HashMap()
        val attribute = ValueHolder.of("")
        val reader = XMLReader()
        reader.addHandler("test") { node: StructuredNode ->
            attributes.putAll(node.getAttributes())
            attribute.set(node.getAttribute("namedAttribute").asString())
        }

        reader.parse(
                ByteArrayInputStream(//language=xml
                        """
                            <doc>
                                <test namedAttribute="abc" namedAttribute2="xyz">1</test>
                            </doc>
                        """.trimIndent().toByteArray()
                )
        )

        reader.parse(ByteArrayInputStream("<doc><test namedAttribute=\"abc\" namedAttribute2=\"xyz\">1</test></doc>".toByteArray()))

        assertEquals(2, attributes.size)
        assertEquals("abc", attribute.get())
    }

    @Test
    fun `Reading non existing attributes does not throw errors`() {
        val attributes: MutableMap<String, String> = HashMap()
        val attribute = ValueHolder.of("wrongValue")
        val reader = XMLReader()
        reader.addHandler("test") { node: StructuredNode ->
            attributes.putAll(node.getAttributes())
            attribute.set(node.getAttribute("namedAttribute").asString())
        }

        reader.parse(
                ByteArrayInputStream(//language=xml
                        """
                            <doc>
                                <test>1</test>
                            </doc>
                        """.trimIndent().toByteArray()
                )
        )

        assertEquals(0, attributes.size)
        assertEquals("", attribute.get())
    }
}
