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
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests the [Json] class.
 */
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
        val json = """{"foo":123,"bar":"baz"}"""
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
        val date = LocalDate.now()
        val time = LocalDateTime.now()
        val node = Json.createObject()
            .put("foo", 123)
            .put("bar", "baz")
            .putPOJO("date", date)
            .putPOJO("time", time)
        val formattedTime = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(time)
        val json = Json.write(node)
        assertEquals("""{"foo":123,"bar":"baz","date":"$date","time":"$formattedTime"}""", json)
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

    @Suppress("DEPRECATION", "removal")
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
    fun `tryGetObjectAt works as expected`() {
        val json = """{ "foo": { "name": "Alice", "age": 30 }, "bar": 123 }"""
        val node = Json.parseObject(json)

        val presentObject: Optional<ObjectNode> = Json.tryGetObjectAt(node, Json.createPointer("foo"))
        assertTrue(presentObject.isPresent)
        assertEquals("Alice", presentObject.get().get("name").asText())

        val notAnObject: Optional<ObjectNode> = Json.tryGetObjectAt(node, Json.createPointer("bar"))
        assertTrue(!notAnObject.isPresent)

        val missingObject: Optional<ObjectNode> = Json.tryGetObjectAt(node, Json.createPointer("baz"))
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
    fun `tryGetArrayAtIndex works with arrays as POJO Nodes`() {
        val node = Json.createArray().addPOJO(listOf(1, 2, 3)).add(123)

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
    fun `tryGetArray works with arrays as POJO Nodes`() {
        val node = Json.createObject().putPOJO("foo", listOf(1, 2, 3)).put("bar", 123)

        val presentArray: Optional<ArrayNode> = Json.tryGetArray(node, "foo")
        assertTrue(presentArray.isPresent)
        assertEquals(3, presentArray.get().size())

        val notAnArray: Optional<ArrayNode> = Json.tryGetArray(node, "bar")
        assertTrue(!notAnArray.isPresent)

        val missingArray: Optional<ArrayNode> = Json.tryGetArray(node, "baz")
        assertTrue(!missingArray.isPresent)
    }

    @Test
    fun `tryGetArrayAt works as expected`() {
        val json = """{ "foo": [1, 2, 3], "bar": 123 }"""
        val node = Json.parseObject(json)

        val presentArray: Optional<ArrayNode> = Json.tryGetArrayAt(node, Json.createPointer("foo"))
        assertTrue(presentArray.isPresent)
        assertEquals(3, presentArray.get().size())

        val notAnArray: Optional<ArrayNode> = Json.tryGetArrayAt(node, Json.createPointer("bar"))
        assertTrue(!notAnArray.isPresent)

        val missingArray: Optional<ArrayNode> = Json.tryGetArrayAt(node, Json.createPointer("baz"))
        assertTrue(!missingArray.isPresent)
    }

    @Test
    fun `tryGetArrayAt works with arrays as POJO Nodes`() {
        val node = Json.createObject()
            .set<ObjectNode>("nested", Json.createObject().putPOJO("foo", listOf(1, 2, 3)).put("bar", 123))

        val presentArray: Optional<ArrayNode> = Json.tryGetArrayAt(node, Json.createPointer("nested/foo"))
        assertTrue(presentArray.isPresent)
        assertEquals(3, presentArray.get().size())

        val notAnArray: Optional<ArrayNode> = Json.tryGetArrayAt(node, Json.createPointer("nested/bar"))
        assertTrue(!notAnArray.isPresent)

        val missingArray: Optional<ArrayNode> = Json.tryGetArrayAt(node, Json.createPointer("nested/baz"))
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

        val nullValue: Optional<JsonNode> = Json.tryGetAt(node, JsonPointer.valueOf("/bar"))
        assertTrue(!nullValue.isPresent)

        val missingValue: Optional<JsonNode> = Json.tryGetAt(node, JsonPointer.valueOf("/baz"))
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
    fun `Json#getValueAmount keeps precision`() {
        val inputJson = """{"number":1.23456789}"""
        val node = Json.parseObject(inputJson)

        val parsedAmount = Json.getValueAmount(node, "number")
        assertEquals(Amount.ofRounded(BigDecimal.valueOf(1.23456789)), parsedAmount)

        val outputJson = Json.createObject().putPOJO("number", parsedAmount).toString()
        assertEquals(outputJson, inputJson)
    }

    @Test
    fun `tryValueString reads string value from string, number and boolean`() {
        val json =
            """{ "number": 123, "string": "blablabla", "null": null, "bool": true, "obj": {"a": "b"}, "array": [] }"""
        val node = Json.parseObject(json)

        assertEquals("123", Json.tryValueString(node, "number").get())
        assertEquals("blablabla", Json.tryValueString(node, "string").get())
        assertTrue(Json.tryValueString(node, "null").isEmpty)
        assertEquals("true", Json.tryValueString(node, "bool").get())
        assertTrue(Json.tryValueString(node, "missingNode").isEmpty)
        assertTrue(Json.tryValueString(node, "obj").isEmpty)
        assertTrue(Json.tryValueString(node, "array").isEmpty)
    }

    @Test
    fun `dates and times can be read properly`() {

        // JS Date stringify representation aka ISO-8601 from strings
        val jsJson = """{ "jsJsonStringifyDate": "2023-01-01T13:37:00.000Z" }"""
        val jsNode = Json.parseObject(jsJson)

        val expectedJsDateTime = LocalDateTime.of(2023, 1, 1, 13, 37, 0, 0)
        assertEquals(expectedJsDateTime, Json.tryValueDateTime(jsNode, "jsJsonStringifyDate").get())
        assertTrue(Json.tryValueDateTime(jsNode, "missingNode").isEmpty)

        // ISO-8601 String representations like generated via Json.write (Jackson -> Jackson)
        val jacksonJson = """{"date":"2023-01-01","time":"2023-01-01T13:37:00.123456"}"""
        val jacksonNode = Json.parseObject(jacksonJson)

        val expectedJacksonDate = LocalDate.of(2023, 1, 1)
        val expectedJacksonDateTime = LocalDateTime.of(2023, 1, 1, 13, 37, 0, 123456000)
        assertEquals(expectedJacksonDate, Json.tryValueDate(jacksonNode, "date").get())
        assertEquals(expectedJacksonDateTime, Json.tryValueDateTime(jacksonNode, "time").get())
    }

    @Test
    fun `JSON pointers can be created properly`() {
        assertEquals(JsonPointer.compile("/foo/bar"), Json.createPointer("foo", "bar"))
        assertEquals(JsonPointer.compile("/foo"), Json.createPointer("foo"))
        assertEquals(JsonPointer.compile("/foo/0/bar"), Json.createPointer("foo", 0, "bar"))
    }

    @Test
    fun `Amounts with large scale get handled properly`() {
        val inputAmount = Amount.ofRounded(BigDecimal("1.23456789"))
        val jsonString = Json.MAPPER.writeValueAsString(inputAmount)
        val parsedAmount = Json.MAPPER.convertValue(jsonString, Amount::class.java)
        assertEquals(inputAmount, parsedAmount)
    }

    @Test
    fun `Read Amount from POJONode`() {
        val inputAmount = Amount.ofRounded(BigDecimal("1.23456789"))
        val objectNode = Json.createObject().putPOJO("amount", inputAmount)
        val amountFromPojo = Json.getValueAmount(objectNode, "amount")
        assertEquals(inputAmount, amountFromPojo)
    }

    @Test
    fun `Read LocalDate from POJONode`() {
        val inputDate = LocalDate.now()
        val objectNode = Json.createObject().putPOJO("localDate", inputDate)
        val localDateFromPojo = Json.tryValueDate(objectNode, "localDate").orElse(null)
        assertEquals(inputDate, localDateFromPojo)
    }

    @Test
    fun `Read LocalDateTime from POJONode`() {
        val inputDateTime = LocalDateTime.now()
        val objectNode = Json.createObject().putPOJO("localDateTime", inputDateTime)
        val localDateTimeFromPojo = Json.tryValueDateTime(objectNode, "localDateTime").orElse(null)
        assertEquals(inputDateTime, localDateTimeFromPojo)
    }

}
