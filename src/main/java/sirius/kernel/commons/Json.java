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
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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
                              .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
                              .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                              .registerModule(new JavaTimeModule());

    static {
        MAPPER.getFactory().setStreamReadConstraints(StreamReadConstraints.builder().maxStringLength(25_000_000).build());
    }

    private Json() {
    }

    /**
     * Creates a new JSON object node.
     *
     * @return a new JSON object node
     */
    @Nonnull
    public static ObjectNode createObject() {
        return MAPPER.createObjectNode();
    }

    /**
     * Creates a new JSON array node.
     *
     * @return a new JSON array node
     */
    @Nonnull
    public static ArrayNode createArray() {
        return MAPPER.createArrayNode();
    }

    /**
     * Creates a pointer for the given property names.
     *
     * @param propertyNames the names of the properties to create a pointer for
     * @return a pointer for the given property names
     */
    public static JsonPointer createPointer(Object... propertyNames) {
        if (propertyNames.length == 0) {
            return JsonPointer.empty();
        }
        String path = "/" + Arrays.stream(propertyNames).map(Object::toString).collect(Collectors.joining("/"));
        return JsonPointer.compile(path);
    }

    /**
     * Creates a new JSON array node with the given elements.
     *
     * @param elements the elements to add to the array
     * @return a new JSON array node
     */
    public static ArrayNode createArray(@Nonnull Collection<?> elements) {
        ArrayNode arrayNode = MAPPER.createArrayNode();
        elements.forEach(arrayNode::addPOJO);
        return arrayNode;
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
     * Converts the given map into a {@link ObjectNode}.
     *
     * @param map the map to convert
     * @return the ObjectNode containing the given map's elements
     */
    public static ObjectNode convertFromMap(Map<String, ?> map) {
        if (map == null) {
            return createObject();
        }
        return MAPPER.convertValue(map, ObjectNode.class);
    }

    /**
     * Converts the value of the given {@link JsonNode} into a Java object.
     *
     * @param node the node to convert
     * @return the Java object representing the given nodes value
     */
    @Nullable
    public static Object convertToJavaObject(@Nullable JsonNode node) {
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
    @Nonnull
    public static Value convertToValue(@Nullable JsonNode node) {
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
    @Nonnull
    public static Stream<JsonNode> streamEntries(@Nonnull ArrayNode arrayNode) {
        return StreamSupport.stream(arrayNode.spliterator(), false);
    }

    /**
     * Creates a deep copy of the given {@link ObjectNode}.
     *
     * @param objectNode the node to clone
     * @return a deep copy of the given node
     */
    @Nonnull
    public static ObjectNode clone(@Nonnull ObjectNode objectNode) {
        return objectNode.deepCopy();
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
    @Nonnull
    public static ObjectNode getObjectAtIndex(@Nonnull ArrayNode arrayNode, int index) {
        return tryGetObjectAtIndex(arrayNode, index).orElseGet(Json::createObject);
    }

    /**
     * Tries to retrieve the {@link ObjectNode} at the given index of the given {@link ArrayNode}.
     *
     * @param arrayNode the node to retrieve the object from
     * @param index     the index of the object to retrieve
     * @return the object at the given index or an empty object if the index is out of bounds or the node is not an object
     */
    @Nonnull
    public static Optional<ObjectNode> tryGetObjectAtIndex(@Nonnull ArrayNode arrayNode, int index) {
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
    @Nonnull
    public static ObjectNode getObject(@Nonnull ObjectNode objectNode, String fieldName) {
        return tryGetObject(objectNode, fieldName).orElseGet(Json::createObject);
    }

    /**
     * Tries to retrieve the {@link ObjectNode} at the given field name of the given {@link ObjectNode}.
     *
     * @param objectNode the node to retrieve the object from
     * @param fieldName  the field name of the object to retrieve
     * @return the object at the given field name or an empty optional if the field does not exist or is not an object
     */
    @Nonnull
    public static Optional<ObjectNode> tryGetObject(@Nonnull ObjectNode objectNode, String fieldName) {
        JsonNode node = objectNode.get(fieldName);
        if (node == null || !node.isObject()) {
            return Optional.empty();
        }
        return Optional.of((ObjectNode) node);
    }

    /**
     * Retrieves the {@link ObjectNode} at the given pointer of the given {@link ObjectNode}.
     * <p>
     * If the field does not exist or is not an object, an empty object is returned.
     *
     * @param jsonNode the node to retrieve the object from
     * @param pointer  the pointer to the object to retrieve
     * @return the object at the given pointer or an empty object if the field does not exist or is not an object
     */
    @Nonnull
    public static ObjectNode getObjectAt(@Nonnull JsonNode jsonNode, JsonPointer pointer) {
        return tryGetObjectAt(jsonNode, pointer).orElseGet(Json::createObject);
    }

    /**
     * Tries to retrieve the {@link ObjectNode} at the given pointer of the given {@link ObjectNode}.
     *
     * @param jsonNode the node to retrieve the object from
     * @param pointer  the pointer to the object to retrieve
     * @return the object at the given pointer or an empty optional if the field does not exist or is not an object
     */
    @Nonnull
    public static Optional<ObjectNode> tryGetObjectAt(@Nonnull JsonNode jsonNode, JsonPointer pointer) {
        JsonNode node = jsonNode.at(pointer);
        if (node == null || !node.isObject() || node.isMissingNode()) {
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
    @Nonnull
    public static ArrayNode getArrayAtIndex(@Nonnull ArrayNode arrayNode, int index) {
        return tryGetArrayAtIndex(arrayNode, index).orElseGet(Json::createArray);
    }

    /**
     * Tries to retrieve the {@link ArrayNode} at the given index of the given {@link ArrayNode}.
     *
     * @param arrayNode the node to retrieve the array from
     * @param index     the index of the array to retrieve
     * @return the array at the given index or an empty optional if the index is out of bounds or the node is not an array
     */
    @Nonnull
    public static Optional<ArrayNode> tryGetArrayAtIndex(@Nonnull ArrayNode arrayNode, int index) {
        JsonNode node = arrayNode.get(index);
        return tryNodeAsArray(node);
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
    @Nonnull
    public static ArrayNode getArray(@Nonnull ObjectNode objectNode, String fieldName) {
        return tryGetArray(objectNode, fieldName).orElseGet(Json::createArray);
    }

    /**
     * Tries to retrieve the {@link ArrayNode} at the given field name of the given {@link ObjectNode}.
     *
     * @param objectNode the node to retrieve the array from
     * @param fieldName  the field name of the array to retrieve
     * @return the array at the given field name or an empty optional if the field does not exist or is not an array
     */
    @Nonnull
    public static Optional<ArrayNode> tryGetArray(@Nonnull ObjectNode objectNode, String fieldName) {
        JsonNode node = objectNode.get(fieldName);
        return tryNodeAsArray(node);
    }

    /**
     * Retrieves the {@link ArrayNode} at the given pointer of the given {@link ObjectNode}.
     * <p>
     * If the field does not exist or is not an array, an empty array is returned.
     *
     * @param jsonNode the node to retrieve the array from
     * @param pointer  the pointer to the array to retrieve
     * @return the array at the given pointer or an empty array if the field does not exist or is not an array
     */
    @Nonnull
    public static ArrayNode getArrayAt(@Nonnull JsonNode jsonNode, JsonPointer pointer) {
        return tryGetArrayAt(jsonNode, pointer).orElseGet(Json::createArray);
    }

    /**
     * Tries to retrieve the {@link ArrayNode} at the given field name of the given {@link ObjectNode}.
     *
     * @param jsonNode the node to retrieve the array from
     * @param pointer  the pointer to the array to retrieve
     * @return the array at the given pointer or an empty optional if the field does not exist or is not an array
     */
    @Nonnull
    public static Optional<ArrayNode> tryGetArrayAt(@Nonnull JsonNode jsonNode, JsonPointer pointer) {
        JsonNode node = jsonNode.at(pointer);
        return tryNodeAsArray(node);
    }

    private static Optional<ArrayNode> tryNodeAsArray(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            return Optional.empty();
        }
        if (node instanceof POJONode pojoNode && pojoNode.getPojo() instanceof Collection<?> collection) {
            return Optional.of(MAPPER.valueToTree(collection));
        }
        return node.isArray() ? Optional.of((ArrayNode) node) : Optional.empty();
    }

    /**
     * Tries to retrieve the {@link JsonNode} at the given index of the given {@link ArrayNode}.
     *
     * @param arrayNode the node to retrieve the value from
     * @param index     the index of the value to retrieve
     * @return the value at the given index or an empty optional if the index is out of bounds or the node is null
     */
    @Nonnull
    public static Optional<JsonNode> tryGetAtIndex(@Nonnull ArrayNode arrayNode, int index) {
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
    @Nonnull
    public static Optional<JsonNode> tryGet(@Nonnull ObjectNode objectNode, String fieldName) {
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
    @Nonnull
    public static Optional<JsonNode> tryGetAt(@Nonnull JsonNode jsonNode, JsonPointer pointer) {
        JsonNode node = jsonNode.at(pointer);
        if (node == null || node.isNull() || node.isMissingNode()) {
            return Optional.empty();
        }
        return Optional.of(node);
    }

    /**
     * Tries to read a String value from the given {@link JsonNode} at the given field name. Returns the string value
     * from string fields and tries to convert number or boolean fields to a string. Objects, arrays or null values
     * return an empty optional.
     *
     * @param jsonNode  the node to retrieve the value from
     * @param fieldName the field name of the value to retrieve
     * @return the String at the given field name or an empty optional if the field does not exist or the node is null
     */
    @Nonnull
    public static Optional<String> tryValueString(@Nonnull JsonNode jsonNode, String fieldName) {
        JsonNode node = jsonNode.get(fieldName);
        if (node == null || node.isNull()) {
            return Optional.empty();
        }
        if (node.isNumber() || node.isBoolean()) {
            return Optional.of(node.asText());
        }
        if (node.isTextual()) {
            return Optional.of(node.textValue());
        }
        return Optional.empty();
    }

    /**
     * Reads an {@link Amount} value from the given {@link JsonNode} at the given field name. In case no
     * number format was used at the JSON source, we try to parse the string value as a machine string.
     *
     * @param jsonNode  the node to retrieve the value from
     * @param fieldName the field name of the value to retrieve
     * @return the value at the given field name or {@link Amount#NOTHING)} if the field does not exist or the node is null
     */
    @Nonnull
    public static Amount getValueAmount(@Nonnull JsonNode jsonNode, String fieldName) {
        JsonNode node = jsonNode.get(fieldName);
        if (node == null || node.isNull()) {
            return Amount.NOTHING;
        }
        if (node.isNumber()) {
            return Amount.ofRounded(node.decimalValue());
        }
        if (node.isTextual()) {
            return Amount.ofMachineString(node.textValue());
        }
        return Amount.NOTHING;
    }

    /**
     * Tries to read a {@link LocalDate} value from the given {@link JsonNode} at the given field name. JSON does
     * not define a date format, so we fall back to ISO-8601.
     *
     * @param jsonNode  the node to retrieve the value from
     * @param fieldName the field name of the value to retrieve
     * @return the value at the given field name or an empty optional if the field does not exist or the node is null
     */
    @Nonnull
    public static Optional<LocalDate> tryValueDate(@Nonnull JsonNode jsonNode, String fieldName) {
        JsonNode node = jsonNode.get(fieldName);
        if (node == null || node.isNull() || !node.isTextual()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(node.textValue(), DateTimeFormatter.ISO_DATE));
        } catch (DateTimeParseException exception) {
            return Optional.empty();
        }
    }

    /**
     * Tries to read a {@link LocalDateTime} value from the given {@link JsonNode} at the given field name. JSON does
     * not define a date format, so we fall back to ISO-8601 which is the default format e.g. used from
     * Javascript using {@code JSON.stringify(new Date())}.
     *
     * @param jsonNode  the node to retrieve the value from
     * @param fieldName the field name of the value to retrieve
     * @return the value at the given field name or an empty optional if the field does not exist or the node is null
     */
    @Nonnull
    public static Optional<LocalDateTime> tryValueDateTime(@Nonnull JsonNode jsonNode, String fieldName) {
        JsonNode node = jsonNode.get(fieldName);
        if (node == null || node.isNull() || !node.isTextual()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDateTime.parse(node.textValue(), DateTimeFormatter.ISO_DATE_TIME));
        } catch (DateTimeParseException exception) {
            return Optional.empty();
        }
    }
}
