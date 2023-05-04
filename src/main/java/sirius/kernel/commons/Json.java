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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;

import java.util.List;
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
     *
     * @param arrayNode the node to retrieve the object from
     * @param index     the index of the object to retrieve
     * @return the object at the given index
     */
    public static ObjectNode getObjectAtIndex(ArrayNode arrayNode, int index) {
        return (ObjectNode) arrayNode.get(index);
    }

    /**
     * Retrieves the {@link ObjectNode} at the given field name of the given {@link ObjectNode}.
     *
     * @param objectNode the node to retrieve the object from
     * @param fieldName  the field name of the object to retrieve
     * @return the object at the given field name
     */
    public static ObjectNode getObject(ObjectNode objectNode, String fieldName) {
        return objectNode.withObject(JsonPointer.SEPARATOR + fieldName);
    }

    /**
     * Retrieves the {@link ArrayNode} at the given index of the given {@link ArrayNode}.
     *
     * @param arrayNode the node to retrieve the array from
     * @param index     the index of the array to retrieve
     * @return the array at the given index
     */
    public static ArrayNode getArrayAtIndex(ArrayNode arrayNode, int index) {
        return (ArrayNode) arrayNode.get(index);
    }

    /**
     * Retrieves the {@link ArrayNode} at the given field name of the given {@link ObjectNode}.
     *
     * @param objectNode the node to retrieve the array from
     * @param fieldName  the field name of the array to retrieve
     * @return the array at the given field name
     */
    public static ArrayNode getArray(ObjectNode objectNode, String fieldName) {
        return objectNode.withArray(JsonPointer.SEPARATOR + fieldName);
    }
}
