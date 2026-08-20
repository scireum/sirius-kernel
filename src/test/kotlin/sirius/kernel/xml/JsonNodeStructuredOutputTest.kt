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
import sirius.kernel.commons.Amount
import sirius.kernel.commons.Json
import sirius.kernel.commons.NumberFormat
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the [JsonNodeStructuredOutput] class.
 */
@ExtendWith(SiriusExtension::class)
internal class JsonNodeStructuredOutputTest {

    @Test
    fun `Objects, arrays and nesting end up in the resulting node`() {
        val node = JsonNodeStructuredOutput.collect { output ->
            output.property("name", "Test")
            output.beginObject("child")
            output.property("name", "Child")
            output.endObject()
            output.beginArray("items")
            output.beginObject("item")
            output.property("pos", 1)
            output.endObject()
            output.beginObject("item")
            output.property("pos", 2)
            output.endObject()
            output.endArray()
        }

        assertEquals("Test", node.path("name").asString())
        assertEquals("Child", node.path("child").path("name").asString())
        assertEquals(2, node.path("items").size())
        assertEquals(2, node.path("items").path(1).path("pos").asInt())
    }

    @Test
    fun `Numbers and booleans are written unquoted`() {
        val node = JsonNodeStructuredOutput.collect { output ->
            output.property("count", 42)
            output.property("ratio", 1.5)
            output.property("flag", true)
            output.property("text", "42")
        }

        assertTrue(node.path("count").isNumber, "count is a number")
        assertTrue(node.path("ratio").isNumber, "ratio is a number")
        assertTrue(node.path("flag").isBoolean, "flag is a boolean")
        assertTrue(node.path("text").isString, "a string which looks like a number stays a string")
    }

    @Test
    fun `A null property becomes a null node rather than being left out`() {
        val node = JsonNodeStructuredOutput.collect { output -> output.property("value", null) }

        assertTrue(node.has("value"), "the property is present")
        assertTrue(node.path("value").isNull, "and it is null")
    }

    @Test
    fun `A property which is not filled is left out entirely`() {
        val node = JsonNodeStructuredOutput.collect { output ->
            output.propertyIfFilled("kept", 42)
            output.propertyIfFilled("skipped", null)
            output.nullsafeProperty("emptied", null)
        }

        assertEquals(42, node.path("kept").asInt())
        assertFalse(node.has("skipped"), "the property is absent rather than null")
        assertEquals("", node.path("emptied").asString(), "whereas nullsafeProperty writes an empty string")
    }

    @Test
    fun `A node handed in is embedded as it is`() {
        val embedded = Json.createObject().put("inner", "value")

        val node = JsonNodeStructuredOutput.collect { output -> output.property("payload", embedded) }

        assertEquals("value", node.path("payload").path("inner").asString())
    }

    @Test
    fun `Values are rendered the way the streaming output renders them`() {
        val node = JsonNodeStructuredOutput.collect { output ->
            output.property("date", LocalDate.of(2026, 8, 4))
            output.property("amount", Amount.of(12.5))
            output.property("empty", Amount.NOTHING)
        }

        assertEquals("2026-08-04", node.path("date").asString())
        assertTrue(node.path("amount").isNumber, "a filled amount is a number")
        assertEquals(12.5, node.path("amount").asDouble())
        assertTrue(node.path("empty").isNull, "an empty amount is null")
    }

    @Test
    fun `A formatted amount is a number, unless it was formatted for a human`() {
        // the streaming output writes the formatted value straight into the document, so a machine format is what
        // callers are expected to ask for -- a localized one must not turn into something a parser would refuse
        val node = JsonNodeStructuredOutput.collect { output ->
            output.amountProperty("machine", Amount.of(1234.5), NumberFormat.MACHINE_TWO_DECIMAL_PLACES, false)
            output.amountProperty("human", Amount.of(1234.5), NumberFormat.TWO_DECIMAL_PLACES, false)
        }

        assertTrue(node.path("machine").isNumber, "a machine-formatted amount is a number")
        assertEquals(1234.5, node.path("machine").asDouble())
        assertTrue(node.path("human").isString, "a localized amount is kept verbatim as a string")
    }

    @Test
    fun `An output which was never written to yields nothing`() {
        val output = JsonNodeStructuredOutput()

        assertEquals(null, output.node)
    }
}
