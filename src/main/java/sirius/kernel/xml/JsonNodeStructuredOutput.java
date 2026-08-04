/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import sirius.kernel.commons.Amount;
import sirius.kernel.commons.Json;
import sirius.kernel.commons.NumberFormat;
import sirius.kernel.commons.Strings;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

/**
 * Assembles a {@link JsonNode} in memory instead of writing to a stream.
 * <p>
 * The implementations of {@link StructuredOutput} write their result somewhere as it is produced: into an XML document
 * ({@link XMLStructuredOutput}) or into a response body (<tt>JSONStructuredOutput</tt> in <tt>sirius-web</tt>). This
 * one keeps it, so that code written to describe a payload can also be used where the payload is needed as a
 * <b>value</b> — embedded into a larger document, handed to a client library, compared against an expected result in a
 * test, or post-processed before it is sent.
 * <p>
 * The point is that the description of the payload does not have to be written twice. A service which renders its
 * result via a {@code StructuredOutput} can be handed one of these, and whatever it emits comes back as a node:
 * <pre>{@code
 * ObjectNode node = JsonNodeStructuredOutput.collect(output -> {
 *     output.property("id", customer.getId());
 *     output.property("name", customer.getName());
 * });
 * }</pre>
 * <p>
 * Values are rendered as the streaming JSON output renders them: a {@link JsonNode} is embedded as it is, an
 * {@link Amount} becomes a number or {@code null}, booleans and numbers stay unquoted, and everything else goes
 * through {@link #transformToStringRepresentation(Object)}, so dates and enums look the same on either.
 */
public class JsonNodeStructuredOutput extends AbstractStructuredOutput {

    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    private final Deque<JsonNode> stack = new ArrayDeque<>();
    private JsonNode root;

    /**
     * Collects what a writer emits into an object.
     * <p>
     * Wraps the whole life cycle — begin, write, end — around the given writer, which is what a caller interested in
     * the resulting node rather than in the output itself is after.
     *
     * @param writer writes the payload, exactly as it would write it to any other output
     * @return the assembled object
     */
    public static ObjectNode collect(Consumer<StructuredOutput> writer) {
        JsonNodeStructuredOutput output = new JsonNodeStructuredOutput();
        output.beginResult();
        writer.accept(output);
        output.endResult();
        return (ObjectNode) output.getNode();
    }

    @Override
    public StructuredOutput beginResult() {
        beginObject("result");
        return this;
    }

    @Override
    public StructuredOutput beginResult(String name) {
        // the name would become the root element in XML; a JSON document has no such element to name
        return beginResult();
    }

    /**
     * Closes the wrapping object and the output, mirroring how the streaming JSON output ends a response.
     */
    @Override
    public void endResult() {
        endObject();
        super.endResult();
    }

    /**
     * Returns the assembled payload once the output has been ended.
     *
     * @return the node which was written, or {@code null} if nothing was written at all
     */
    public JsonNode getNode() {
        return root;
    }

    @Override
    protected void startObject(String name, Attribute... attributes) {
        ObjectNode node = Json.createObject();
        attach(name, node);
        stack.push(node);
        if (attributes != null) {
            for (Attribute attribute : attributes) {
                property(attribute.getName(), attribute.getValue());
            }
        }
    }

    @Override
    protected void endObject(String name) {
        pop();
    }

    @Override
    protected void startArray(String name) {
        ArrayNode node = Json.createArray();
        attach(name, node);
        stack.push(node);
    }

    @Override
    protected void endArray(String name) {
        pop();
    }

    @Override
    public void writeProperty(String name, Object data) {
        if (data instanceof JsonNode node) {
            put(name, node);
            return;
        }
        if (data == null) {
            put(name, NODES.nullNode());
            return;
        }
        if (data instanceof Amount amount) {
            put(name,
                amount.isFilled() ? NODES.numberNode(new BigDecimal(amount.toMachineString())) : NODES.nullNode());
            return;
        }
        if (data instanceof Boolean value) {
            put(name, NODES.booleanNode(value));
            return;
        }
        if (data instanceof Number value) {
            put(name, NODES.numberNode(new BigDecimal(value.toString())));
            return;
        }
        put(name, NODES.stringNode(transformToStringRepresentation(data)));
    }

    /**
     * Writes an amount which {@link #amountProperty(String, Amount, NumberFormat, boolean)} has already formatted.
     * <p>
     * A machine-formatted amount becomes a number, as it does when the output is streamed. One formatted for a human
     * ("1.234,50") cannot be, and is kept as a string rather than being rejected or written out as something a parser
     * would refuse — a caller which asked for a localized format presumably means it.
     */
    @Override
    protected void writeAmountProperty(String name, String formattedAmount) {
        if (Strings.isEmpty(formattedAmount)) {
            put(name, NODES.nullNode());
            return;
        }

        try {
            put(name, NODES.numberNode(new BigDecimal(formattedAmount)));
        } catch (NumberFormatException exception) {
            put(name, NODES.stringNode(formattedAmount));
        }
    }

    /**
     * Hangs a freshly opened container into its parent, or records it as the root when there is none.
     */
    private void attach(String name, JsonNode node) {
        if (stack.isEmpty()) {
            root = node;
            return;
        }
        put(name, node);
    }

    /**
     * Adds a value to the current container: named when it is an object, positional when it is an array.
     */
    private void put(String name, JsonNode value) {
        JsonNode current = stack.peek();
        if (current instanceof ArrayNode array) {
            array.add(value);
        } else if (current instanceof ObjectNode object) {
            object.set(name, value);
        }
    }

    private void pop() {
        JsonNode finished = stack.pop();
        if (stack.isEmpty()) {
            root = finished;
        }
    }
}
