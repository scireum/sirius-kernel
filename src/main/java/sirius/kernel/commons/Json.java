/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Provides a simple wrapper for Jackson to provide a more fluent API with some additional functions that provide useful
 * shortcuts.
 * <p>
 * Also, this class provides a central place to configure the underlying {@link ObjectMapper} for uniform parsing
 * and writing of JSON strings.
 */
public class Json {

    /**
     * Contains the logger used by this class.
     */
    public static final Log LOG = Log.get("json");

    /**
     * Contains the default object mapper used by this class. This is used to parse and write JSON strings.
     * <p>
     * Note that this mapper is configured to allow single quotes and unquoted field names when parsing JSON strings.
     */
    public static final ObjectMapper MAPPER =
            new ObjectMapper().configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
                              .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

    private Json() {
    }

    /**
     * Creates a new JSON object node.
     *
     * @return a new JSON object node
     */
    public static ObjectNode createObject() {
        return MAPPER.createObjectNode();
    }

    /**
     * Creates a new JSON array node.
     *
     * @return a new JSON array node
     */
    public static ArrayNode createArray() {
        return MAPPER.createArrayNode();
    }

    /**
     * Parses the given JSON string into an {@link ObjectNode}.
     *
     * @param json the JSON string to parse
     * @return the parsed JSON string as object node
     */
    public static ObjectNode parseObject(String json) {
        try {
            return tryParseObject(json);
        } catch (JsonProcessingException exception) {
            throw Exceptions.handle(LOG, exception);
        }
    }

    /**
     * Tries to parse the given JSON string into an {@link ObjectNode}.
     *
     * @param json the JSON string to parse
     * @return the parsed JSON string as object node
     * @throws JsonProcessingException in case of a malformed JSON string
     */
    public static ObjectNode tryParseObject(String json) throws JsonProcessingException {
        return MAPPER.readValue(json, ObjectNode.class);
    }

    /**
     * Parses the given JSON string into an {@link ArrayNode}.
     *
     * @param json the JSON string to parse
     * @return the parsed JSON string as array node
     */
    public static ArrayNode parseArray(String json) {
        try {
            return tryParseArray(json);
        } catch (JsonProcessingException exception) {
            throw Exceptions.handle(LOG, exception);
        }
    }

    /**
     * Tries to parse the given JSON string into an {@link ArrayNode}.
     *
     * @param json the JSON string to parse
     * @return the parsed JSON string as array node
     * @throws JsonProcessingException in case of a malformed JSON string
     */
    public static ArrayNode tryParseArray(String json) throws JsonProcessingException {
        return MAPPER.readValue(json, ArrayNode.class);
    }

    /**
     * Converts the given {@link JsonNode} into a JSON string.
     *
     * @param objectNode the node to convert
     * @return the JSON string representing the given node
     */
    public static String write(JsonNode objectNode) {
        try {
            return tryWrite(objectNode);
        } catch (JsonProcessingException exception) {
            throw Exceptions.handle(LOG, exception);
        }
    }

    /**
     * Tries to convert the given {@link JsonNode} into a JSON string.
     *
     * @param objectNode the node to convert
     * @return the JSON string representing the given node
     * @throws JsonProcessingException in case the given node cannot be converted
     */
    public static String tryWrite(JsonNode objectNode) throws JsonProcessingException {
        return MAPPER.writeValueAsString(objectNode);
    }

    /**
     * Converts the given {@link JsonNode} into a pretty printed (formatted) JSON string.
     *
     * @param objectNode the node to convert
     * @return the pretty printed JSON string representing the given node
     */
    public static String writePretty(JsonNode objectNode) {
        try {
            return tryWritePretty(objectNode);
        } catch (JsonProcessingException exception) {
            throw Exceptions.handle(LOG, exception);
        }
    }

    /**
     * Tries to convert the given {@link JsonNode} into a pretty printed (formatted) JSON string.
     *
     * @param objectNode the node to convert
     * @return the pretty printed JSON string representing the given node
     * @throws JsonProcessingException in case the given node cannot be converted
     */
    public static String tryWritePretty(JsonNode objectNode) throws JsonProcessingException {
        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(objectNode);
    }

    /**
     * Converts the given {@link ArrayNode} into a list of the given type.
     *
     * @param arrayNode the node to convert
     * @param clazz     the type of the list elements
     * @param <T>       the type of the list elements
     * @return the list of the given type
     */
    public static <T> List<T> convertToList(ArrayNode arrayNode, Class<T> clazz) {
        return MAPPER.convertValue(arrayNode, MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
    }

    /**
     * Converts the given {@link ObjectNode} into a map.
     *
     * @param objectNode the node to convert
     * @return the inner map of the ObjectNode
     */
    public static Map<String, Object> convertToMap(ObjectNode objectNode) {
        return MAPPER.convertValue(objectNode, new TypeReference<Map<String, Object>>() {
        });
    }

    /**
     * Converts the value of the given {@link JsonNode} into a Java object.
     *
     * @param node the node to convert
     * @return the Java object representing the given nodes value
     */
    public static Object convertToJavaObject(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        return MAPPER.convertValue(node, Object.class);
    }

    /**
     * Converts the value of the given {@link JsonNode} into a {@link Value}.
     *
     * @param node the node to convert
     * @return the value representing the given nodes value
     */
    public static Value convertToValue(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return Value.EMPTY;
        }
        return Value.of(MAPPER.convertValue(node, Object.class));
    }

    /**
     * Streams the entries of the given {@link ArrayNode}.
     *
     * @param arrayNode the node to stream
     * @return a stream of the entries of the given node
     */
    public static Stream<JsonNode> streamEntries(ArrayNode arrayNode) {
        return StreamSupport.stream(arrayNode.spliterator(), false);
    }

    /**
     * Creates a deep copy of the given {@link ObjectNode}.
     *
     * @param objectNode the node to clone
     * @return a deep copy of the given node
     */
    public static ObjectNode clone(ObjectNode objectNode) {
        return parseObject(write(objectNode));
    }

    /**
     * Retrieves the {@link ObjectNode} at the given index of the given {@link ArrayNode}.
     * <p>
     * If the index is out of bounds or the node is not an object, an empty object is returned.
     *
     * @param arrayNode the node to retrieve the object from
     * @param index     the index of the object to retrieve
     * @return the object at the given index or an empty object if the index is out of bounds or the node is not an object
     */
    public static ObjectNode getObjectAtIndex(ArrayNode arrayNode, int index) {
        return tryGetObjectAtIndex(arrayNode, index).orElseGet(Json::createObject);
    }

    /**
     * Tries to retrieve the {@link ObjectNode} at the given index of the given {@link ArrayNode}.
     *
     * @param arrayNode the node to retrieve the object from
     * @param index     the index of the object to retrieve
     * @return the object at the given index or an empty object if the index is out of bounds or the node is not an object
     */
    public static Optional<ObjectNode> tryGetObjectAtIndex(ArrayNode arrayNode, int index) {
        JsonNode node = arrayNode.get(index);
        if (node == null || !node.isObject()) {
            return Optional.empty();
        }
        return Optional.of((ObjectNode) node);
    }

    /**
     * Retrieves the {@link ObjectNode} at the given field name of the given {@link ObjectNode}.
     * <p>
     * If the field does not exist or is not an object, an empty object is returned.
     *
     * @param objectNode the node to retrieve the object from
     * @param fieldName  the field name of the object to retrieve
     * @return the object at the given field name or an empty object if the field does not exist or is not an object
     */
    public static ObjectNode getObject(ObjectNode objectNode, String fieldName) {
        return tryGetObject(objectNode, fieldName).orElseGet(Json::createObject);
    }

    /**
     * Tries to retrieve the {@link ObjectNode} at the given field name of the given {@link ObjectNode}.
     *
     * @param objectNode the node to retrieve the object from
     * @param fieldName  the field name of the object to retrieve
     * @return the object at the given field name or an empty optional if the field does not exist or is not an object
     */
    public static Optional<ObjectNode> tryGetObject(ObjectNode objectNode, String fieldName) {
        JsonNode node = objectNode.get(fieldName);
        if (node == null || !node.isObject()) {
            return Optional.empty();
        }
        return Optional.of((ObjectNode) node);
    }

    /**
     * Retrieves the {@link ArrayNode} at the given index of the given {@link ArrayNode}.
     *
     * @param arrayNode the node to retrieve the array from
     * @param index     the index of the array to retrieve
     * @return the array at the given index
     */
    public static ArrayNode getArrayAtIndex(ArrayNode arrayNode, int index) {
        return tryGetArrayAtIndex(arrayNode, index).orElseGet(Json::createArray);
    }

    /**
     * Tries to retrieve the {@link ArrayNode} at the given index of the given {@link ArrayNode}.
     *
     * @param arrayNode the node to retrieve the array from
     * @param index     the index of the array to retrieve
     * @return the array at the given index or an empty optional if the index is out of bounds or the node is not an array
     */
    public static Optional<ArrayNode> tryGetArrayAtIndex(ArrayNode arrayNode, int index) {
        JsonNode node = arrayNode.get(index);
        if (node == null || !node.isArray()) {
            return Optional.empty();
        }
        return Optional.of((ArrayNode) node);
    }

    /**
     * Retrieves the {@link ArrayNode} at the given field name of the given {@link ObjectNode}.
     * <p>
     * If the field does not exist or is not an array, an empty array is returned.
     *
     * @param objectNode the node to retrieve the array from
     * @param fieldName  the field name of the array to retrieve
     * @return the array at the given field name or an empty array if the field does not exist or is not an array
     */
    public static ArrayNode getArray(ObjectNode objectNode, String fieldName) {
        return tryGetArray(objectNode, fieldName).orElseGet(Json::createArray);
    }

    /**
     * Tries to retrieve the {@link ArrayNode} at the given field name of the given {@link ObjectNode}.
     *
     * @param objectNode the node to retrieve the array from
     * @param fieldName  the field name of the array to retrieve
     * @return the array at the given field name or an empty optional if the field does not exist or is not an array
     */
    public static Optional<ArrayNode> tryGetArray(ObjectNode objectNode, String fieldName) {
        JsonNode node = objectNode.get(fieldName);
        if (node == null || !node.isArray()) {
            return Optional.empty();
        }
        return Optional.of((ArrayNode) node);
    }

    /**
     * Tries to retrieve the {@link JsonNode} at the given index of the given {@link ArrayNode}.
     *
     * @param arrayNode the node to retrieve the value from
     * @param index     the index of the value to retrieve
     * @return the value at the given index or an empty optional if the index is out of bounds or the node is null
     */
    public static Optional<JsonNode> tryGetAtIndex(ArrayNode arrayNode, int index) {
        JsonNode node = arrayNode.get(index);
        if (node == null || node.isNull()) {
            return Optional.empty();
        }
        return Optional.of(node);
    }

    /**
     * Tries to retrieve the {@link JsonNode} at the given field name of the given {@link ObjectNode}.
     *
     * @param objectNode the node to retrieve the value from
     * @param fieldName  the field name of the value to retrieve
     * @return the value at the given field name or an empty optional if the field does not exist or the node is null
     */
    public static Optional<JsonNode> tryGet(ObjectNode objectNode, String fieldName) {
        JsonNode node = objectNode.get(fieldName);
        if (node == null || node.isNull()) {
            return Optional.empty();
        }
        return Optional.of(node);
    }

    /**
     * Tries to retrieve the {@link JsonNode} at the given pointer of the given {@link JsonNode}.
     *
     * @param jsonNode the node to retrieve the value from
     * @param pointer  the pointer to the value to retrieve
     * @return the value at the given pointer or an empty optional if the pointer does not exist or the node is null
     */
    public static Optional<JsonNode> tryGetAt(JsonNode jsonNode, JsonPointer pointer) {
        JsonNode node = jsonNode.at(pointer);
        if (node == null || node.isNull()) {
            return Optional.empty();
        }
        return Optional.of(node);
    }
}
