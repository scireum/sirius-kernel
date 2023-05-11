/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons

import com.fasterxml.jackson.core.JsonPointer
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class JsonTest {

    @Test
    fun `empty object is created properly`() {
        val json: ObjectNode = Json.createObject()
        assert(json.isEmpty)
        assertEquals(0, json.size())
        assertEquals("{}", Json.write(json))
    }

    @Test
    fun `empty array is created properly`() {
        val json: ArrayNode = Json.createArray()
        assert(json.isEmpty)
        assertEquals(0, json.size())
        assertEquals("[]", Json.write(json))
    }

    @Test
    fun `valid object is parsed properly`() {
        val json = """{ "foo": 123, "bar": "baz" }"""
        val node = Json.parseObject(json)
        assertEquals(2, node.size())
        assertEquals(123, node["foo"].asInt())
        assertEquals("baz", node["bar"].asText())
    }

    @Test
    fun `nearly valid object is parsed leniently according to mapper settings`() {
        val json = """{ foo: 123, bar: 'baz' }"""
        val node = Json.parseObject(json)
        assertEquals(2, node.size())
        assertEquals(123, node["foo"].asInt())
        assertEquals("baz", node["bar"].asText())
    }

    @Test
    fun `exception is thrown when parsing invalid object`() {
        val invalidJson = "[1, 2, 3]"
        assertThrows(JsonProcessingException::class.java) {
            Json.tryParseObject(invalidJson)
        }
    }

    @Test
    fun `valid array is parsed properly`() {
        val json = "[1, 2, 3]"
        val array = Json.parseArray(json)
        assertEquals(3, array.size())
        assertEquals(1, array[0].asInt())
        assertEquals(2, array[1].asInt())
        assertEquals(3, array[2].asInt())
    }

    @Test
    fun `exception is thrown when parsing invalid array`() {
        val invalidJson = """{ "foo": 123, "bar": "baz" }"""
        assertThrows(JsonProcessingException::class.java) {
            Json.tryParseArray(invalidJson)
        }
    }

    @Test
    fun `valid object is written properly`() {
        val node = Json.createObject().put("foo", 123).put("bar", "baz")
        val json = Json.write(node)
        assertEquals("""{"foo":123,"bar":"baz"}""", json)
    }

    @Test
    fun `valid array is written properly`() {
        val node = Json.createArray().add(1).add(2).add(3)
        val json = Json.write(node)
        assertEquals("[1,2,3]", json)
    }

    @Test
    fun `valid object is pretty printed properly`() {
        val node = Json.createObject().put("foo", 123).put("bar", "baz")
        val prettyJson = Json.writePretty(node)
        assertEquals("{\n  \"foo\" : 123,\n  \"bar\" : \"baz\"\n}", prettyJson)
    }

    @Test
    fun `valid array can be converted to java list`() {
        val array = Json.createArray().add(1).add(2).add(3)
        val list = Json.convertToList(array, Int::class.java)
        assertEquals(mutableListOf(1, 2, 3), list)
    }

    @Test
    fun `valid object can be converted to java map`() {
        val node = Json.createObject().put("foo", 123).put("bar", "baz")
        val map = Json.convertToMap(node)
        assertEquals(mutableMapOf("foo" to 123, "bar" to "baz"), map)
    }

    @Test
    fun `entries of array can be streamed properly`() {
        val array = Json.createArray().add(1).add(2).add(3)
        val list = mutableListOf<Int>()
        Json.streamEntries(array).forEach { list.add(it.asInt()) }
        assertEquals(mutableListOf(1, 2, 3), list)
    }

    @Test
    fun `nested object is cloned properly`() {
        val node = Json.createObject().put("foo", 123).put("bar", "baz")
        node.withArray("array").add(1).add(2).add(3)

        val clone = Json.clone(node)
        assertEquals(node.size(), clone.size())
        assertEquals(node.get("foo"), clone.get("foo"))
        assertEquals(node.get("bar"), clone.get("bar"))
        assertEquals(node.get("array"), clone.get("array"))
        assertEquals(node.get("array").isArray, clone.get("array").isArray)
        assertEquals(node.get("array").size(), clone.get("array").size())
    }

    @Test
    fun `tryGetObjectAtIndex works as expected`() {
        val json = """[ { "name": "Alice", "age": 30 }, 123 ]"""
        val node = Json.parseArray(json)

        val presentObject: Optional<ObjectNode> = Json.tryGetObjectAtIndex(node, 0)
        assertTrue(presentObject.isPresent)
        assertEquals("Alice", presentObject.get().get("name").asText())

        val notAnObject: Optional<ObjectNode> = Json.tryGetObjectAtIndex(node, 1)
        assertTrue(!notAnObject.isPresent)

        val missingObject: Optional<ObjectNode> = Json.tryGetObjectAtIndex(node, 2)
        assertTrue(!missingObject.isPresent)
    }

    @Test
    fun `tryGetObject works as expected`() {
        val json = """{ "foo": { "name": "Alice", "age": 30 }, "bar": 123 }"""
        val node = Json.parseObject(json)

        val presentObject: Optional<ObjectNode> = Json.tryGetObject(node, "foo")
        assertTrue(presentObject.isPresent)
        assertEquals("Alice", presentObject.get().get("name").asText())

        val notAnObject: Optional<ObjectNode> = Json.tryGetObject(node, "bar")
        assertTrue(!notAnObject.isPresent)

        val missingObject: Optional<ObjectNode> = Json.tryGetObject(node, "baz")
        assertTrue(!missingObject.isPresent)
    }

    @Test
    fun `tryGetArrayAtIndex works as expected`() {
        val json = """[ [1, 2, 3], 123 ]"""
        val node = Json.parseArray(json)

        val presentArray: Optional<ArrayNode> = Json.tryGetArrayAtIndex(node, 0)
        assertTrue(presentArray.isPresent)
        assertEquals(3, presentArray.get().size())

        val notAnArray: Optional<ArrayNode> = Json.tryGetArrayAtIndex(node, 1)
        assertTrue(!notAnArray.isPresent)

        val missingArray: Optional<ArrayNode> = Json.tryGetArrayAtIndex(node, 2)
        assertTrue(!missingArray.isPresent)
    }

    @Test
    fun `tryGetArray works as expected`() {
        val json = """{ "foo": [1, 2, 3], "bar": 123 }"""
        val node = Json.parseObject(json)

        val presentArray: Optional<ArrayNode> = Json.tryGetArray(node, "foo")
        assertTrue(presentArray.isPresent)
        assertEquals(3, presentArray.get().size())

        val notAnArray: Optional<ArrayNode> = Json.tryGetArray(node, "bar")
        assertTrue(!notAnArray.isPresent)

        val missingArray: Optional<ArrayNode> = Json.tryGetArray(node, "baz")
        assertTrue(!missingArray.isPresent)
    }

    @Test
    fun `tryGetAtIndex works as expected`() {
        val json = """[1, null]"""
        val node = Json.parseArray(json)

        val presentValue: Optional<JsonNode> = Json.tryGetAtIndex(node, 0)
        assertTrue(presentValue.isPresent)
        assertEquals(1, presentValue.get().asInt())

        val nullValue: Optional<JsonNode> = Json.tryGetAtIndex(node, 1)
        assertTrue(!nullValue.isPresent)

        val missingValue: Optional<JsonNode> = Json.tryGetAtIndex(node, 2)
        assertTrue(!missingValue.isPresent)
    }

    @Test
    fun `tryGet works as expected`() {
        val json = """{ "foo": 1, "bar": null }"""
        val node = Json.parseObject(json)

        val presentValue: Optional<JsonNode> = Json.tryGet(node, "foo")
        assertTrue(presentValue.isPresent)
        assertEquals(1, presentValue.get().asInt())

        val nullValue: Optional<JsonNode> = Json.tryGet(node, "bar")
        assertTrue(!nullValue.isPresent)

        val missingValue: Optional<JsonNode> = Json.tryGet(node, "baz")
        assertTrue(!missingValue.isPresent)
    }

    @Test
    fun `tryGetAt works as expected`() {
        val json = """{ "foo": [ { "name": "Alice", "age": 30 } ], "bar": null }"""
        val node = Json.parseObject(json)

        val presentValue: Optional<JsonNode> = Json.tryGetAt(node, JsonPointer.valueOf("/foo/0/name"))
        assertTrue(presentValue.isPresent)
        assertEquals("Alice", presentValue.get().asText())

        val nullValue: Optional<JsonNode> = Json.tryGet(node, "bar")
        assertTrue(!nullValue.isPresent)

        val missingValue: Optional<JsonNode> = Json.tryGet(node, "baz")
        assertTrue(!missingValue.isPresent)
    }


    @Test
    fun `getValueAmount reads value from number and string`() {
        val json =
                """{"number_1": 123,"number_2": -123,"number_3": 12.3,"number_4": 1.0E+2,"string": "123","null":null}"""
        val node = Json.parseObject(json)

        assertEquals(Amount.of(123L), Json.getValueAmount(node, "number_1"))
        assertEquals(Amount.of(-123L), Json.getValueAmount(node, "number_2"))
        assertEquals(Amount.of(12.3), Json.getValueAmount(node, "number_3"))
        assertEquals(Amount.of(100L), Json.getValueAmount(node, "number_4"))
        assertEquals(Amount.of(123L), Json.getValueAmount(node, "string"))
        assertTrue(Json.getValueAmount(node, "null").isEmpty)
        assertTrue(Json.getValueAmount(node, "missingNode").isEmpty)
    }

    @Test
    fun `tryValueString reads string value from string, number and boolean`() {
        val json = """{ "number": 123, "string": "blablabla", "null": null, "bool": true }"""
        val node = Json.parseObject(json)

        assertEquals("123", Json.tryValueString(node, "number").get())
        assertEquals("blablabla", Json.tryValueString(node, "string").get())
        assertTrue(Json.tryValueString(node, "null").isEmpty)
        assertEquals("true", Json.tryValueString(node, "bool").get())
        assertTrue(Json.tryValueString(node, "missingNode").isEmpty)
    }

    @Test
    fun `tryValueDateTime reads JS Date stringify representation aka ISO-8601 from strings`() {
        val json = """{ "jsJsonStringifyDate": "2023-05-10T09:00:00.000Z" }"""
        val node = Json.parseObject(json)

        val expected = LocalDateTime.of(2023, 5, 10, 9, 0, 0)
        assertEquals(expected, Json.tryValueDateTime(node, "jsJsonStringifyDate").get())
        assertTrue(Json.tryValueDateTime(node, "missingNode").isEmpty)
    }
}
