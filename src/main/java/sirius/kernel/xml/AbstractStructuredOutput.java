/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import sirius.kernel.Sirius;
import sirius.kernel.async.ExecutionPoint;
import sirius.kernel.commons.Amount;
import sirius.kernel.commons.NumberFormat;
import sirius.kernel.commons.Strings;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.HandledException;
import sirius.kernel.health.Log;
import sirius.kernel.nls.NLS;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.RecordComponent;
import java.nio.channels.ClosedChannelException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Basic implementation of <tt>StructuredOutput</tt>, taking care of all output independent boilerplate code.
 */
public abstract class AbstractStructuredOutput implements StructuredOutput {

    private static final Attribute[] EMPTY_ATTRIBUTES_ARRAY = new Attribute[0];
    protected List<Element> nesting = new ArrayList<>();

    /**
     * Types used by internal bookkeeping
     */
    protected enum ElementType {
        UNKNOWN, OBJECT, ARRAY
    }

    /**
     * Used by internal bookkeeping, to close elements property
     */
    protected static class Element {

        private final ElementType type;
        private final String name;
        private boolean empty = true;

        protected Element(ElementType type, String name) {
            this.type = type;
            this.name = name;
        }

        protected boolean isEmpty() {
            return empty;
        }

        protected void setEmpty(boolean empty) {
            this.empty = empty;
        }

        protected ElementType getType() {
            return type;
        }

        protected String getName() {
            return name;
        }
    }

    /**
     * Returns the type of the current element.
     *
     * @return the type of the current element
     */
    protected ElementType getCurrentType() {
        if (nesting.isEmpty()) {
            return ElementType.UNKNOWN;
        }
        return nesting.get(0).getType();
    }

    /**
     * Determines whether the current element is empty
     *
     * @return <tt>true</tt> if the current element has no content or children, <tt>false</tt> otherwise
     */
    public boolean isCurrentObjectEmpty() {
        if (nesting.isEmpty()) {
            return true;
        }
        return nesting.get(0).isEmpty();
    }

    @Override
    public StructuredOutput beginArray(String name) {
        startArray(name);
        if (!nesting.isEmpty()) {
            nesting.get(0).setEmpty(false);
        }
        nesting.add(0, new Element(ElementType.ARRAY, name));

        return this;
    }

    @Override
    public StructuredOutput array(@Nonnull String name, @Nonnull String elementName, @Nonnull Collection<?> array) {
        beginArray(name);
        for (Object o : array) {
            property(elementName, o);
        }
        endArray();
        return this;
    }

    @Override
    public <E> StructuredOutput array(@Nonnull String name,
                                      @Nonnull Collection<E> array,
                                      BiConsumer<StructuredOutput, E> arrayConsumer) {
        beginArray(name);
        for (E e : array) {
            arrayConsumer.accept(this, e);
        }
        endArray();

        return this;
    }

    /**
     * Must be implemented by subclasses to start a new array.
     *
     * @param name the name of the array property.
     */
    protected abstract void startArray(String name);

    /**
     * Must be implemented by subclasses to start a new object.
     *
     * @param name       the name of the object
     * @param attributes the attributes of the object
     */
    protected abstract void startObject(String name, Attribute... attributes);

    /**
     * Must be implemented by subclasses to end an array.
     *
     * @param name the name of the array property to close
     */
    protected abstract void endArray(String name);

    /**
     * Must be implemented by subclasses to end an object.
     *
     * @param name the name of the object to close
     */
    protected abstract void endObject(String name);

    @Override
    public StructuredOutput object(@Nonnull String name, Record object) {
        if (object == null) {
            return this;
        }

        beginObject(name);
        for (RecordComponent component : object.getClass().getRecordComponents()) {
            try {
                property(component.getName(), component.getAccessor().invoke(object));
            } catch (Exception e) {
                throw new IllegalArgumentException(Strings.apply(
                        "Failed to serialize record component %s of record %s with type %s: %s",
                        component.getName(),
                        object,
                        object.getClass().getName(),
                        e.getMessage()));
            }
        }

        return endObject();
    }

    /**
     * Must be implemented by subclasses to generate a property.
     *
     * @param name  the name of the property
     * @param value the value of the property
     */
    protected abstract void writeProperty(String name, Object value);

    /**
     * Writes formatted amounts. Must be implemented by subclasses to generate a property.
     *
     * @param name            the name of the property
     * @param formattedAmount the amount formatted with either {@link Amount#toString(NumberFormat)}
     *                        or {@link Amount#toSmartRoundedString(NumberFormat)}
     */
    protected abstract void writeAmountProperty(String name, String formattedAmount);

    /**
     * Creates the string representation to be used when outputting the given value.
     *
     * @param value the value to represent
     * @return the machine-readable string representation of the value
     */
    protected String transformToStringRepresentation(Object value) {
        // We preserve some objects here because:
        //  * String: as NLS.toMachineString performs an implicit trim which might be unwanted here.
        //  * LocalDateTime: as we want a "full ISO" format "date"T"time" and not "date" "time" as NLS.toMachineString
        //    does
        if ((value instanceof String) || (value instanceof LocalDateTime)) {
            return value.toString();
        } else {
            return NLS.toMachineString(value);
        }
    }

    @Override
    public StructuredOutput beginObject(String name, Attribute... attributes) {
        startObject(name, attributes);
        if (!nesting.isEmpty()) {
            nesting.get(0).setEmpty(false);
        }
        nesting.add(0, new Element(ElementType.OBJECT, name));
        return this;
    }

    /**
     * Used to fluently create a {@link #beginObject(String, Attribute...)}.
     */
    public class TagBuilder {
        private final List<Attribute> attributes = new ArrayList<>();
        private final String name;

        /**
         * Creates a new TabBuilder with the given tag name
         *
         * @param name the name of the resulting tag
         */
        public TagBuilder(String name) {
            this.name = name;
        }

        /**
         * Adds an attribute to the tag
         *
         * @param name  the name of the attribute to add
         * @param value the value of the attribute to add
         * @return <tt>this</tt> to fluently add more attributes
         */
        public TagBuilder addAttribute(@Nonnull String name, @Nullable String value) {
            attributes.add(Attribute.set(name, value));
            return this;
        }

        /**
         * Adds an attribute to the tag
         *
         * @param namespace the namespace of the attribute to add
         * @param name      the name of the attribute to add
         * @param value     the value of the attribute to add
         * @return <tt>this</tt> to fluently add more attributes
         */
        public TagBuilder addAttribute(@Nonnull String namespace, @Nonnull String name, @Nullable String value) {
            attributes.add(Attribute.set(namespace, name, value));
            return this;
        }

        /**
         * Finally creates the tag or object with the given name and attributes.
         */
        public void build() {
            beginObject(name, attributes.toArray(EMPTY_ATTRIBUTES_ARRAY));
        }
    }

    /**
     * Creates a new object using the returned tag builder
     *
     * @param name the name of the object to create
     * @return a tag builder to fluently create a new object
     */
    @CheckReturnValue
    public TagBuilder buildObject(@Nonnull String name) {
        return new TagBuilder(name);
    }

    @Override
    public StructuredOutput endArray() {
        if (nesting.isEmpty()) {
            throw new IllegalArgumentException("Invalid result structure. No array to close");
        }
        Element e = nesting.get(0);
        nesting.remove(0);
        if (e.getType() != ElementType.ARRAY) {
            throw new IllegalArgumentException("Invalid result structure. No array to close");
        }
        endArray(e.getName());
        return this;
    }

    @Override
    public StructuredOutput endObject() {
        if (nesting.isEmpty()) {
            throw new IllegalArgumentException("Invalid result structure. No object to close");
        }
        Element e = nesting.get(0);
        nesting.remove(0);
        if (e.getType() != ElementType.OBJECT) {
            throw new IllegalArgumentException("Invalid result structure. No object to close");
        }
        endObject(e.getName());
        return this;
    }

    @Override
    public void endResult() {
        if (!nesting.isEmpty()) {
            throw new IllegalArgumentException("Invalid result structure. Cannot close result. Objects are still "
                                               + "open"
                                               + ".");
        }
    }

    @Override
    public StructuredOutput property(String name, Object data) {
        validateResultStructure();
        warnImproperAmountUsage(name, data);
        if (data instanceof Record castRecord) {
            object(name, castRecord);
        } else {
            writeProperty(name, data);
        }
        nesting.get(0).setEmpty(false);
        return this;
    }

    protected void warnImproperAmountUsage(String name, Object data) {
        if (!Sirius.isProd() && data instanceof Amount) {
            Log.SYSTEM.WARN("""
                                    Use StructuredOutput.amountProperty to output Amounts to guarantee proper numeric formatting.
                                    Property name: '%s'
                                    %s
                                    """, name, ExecutionPoint.fastSnapshot());
        }
    }

    @Override
    public StructuredOutput nullsafeProperty(@Nonnull String name, @Nullable Object data) {
        property(name, data != null ? data : "");
        return this;
    }

    @Override
    public StructuredOutput amountProperty(@Nonnull String name,
                                           @Nullable Amount amount,
                                           @Nonnull NumberFormat numberFormat,
                                           boolean smartRound) {
        if (amount == null || amount.isEmpty()) {
            return property(name, null);
        }

        validateResultStructure();

        if (smartRound) {
            writeAmountProperty(name, amount.toSmartRoundedString(numberFormat).asString());
        } else {
            writeAmountProperty(name, amount.toString(numberFormat).asString());
        }

        nesting.get(0).setEmpty(false);
        return this;
    }

    private void validateResultStructure() {
        if (getCurrentType() != ElementType.OBJECT && getCurrentType() != ElementType.ARRAY) {
            throw new IllegalArgumentException("Invalid result structure. Cannot place a property here.");
        }
    }

    protected HandledException handleOutputException(Exception exception) {
        if (exception instanceof ClosedChannelException closedChannelException) {
            return handleClosedChannel(closedChannelException);
        } else if (exception.getCause() instanceof ClosedChannelException closedChannelException) {
            return handleClosedChannel(closedChannelException);
        } else {
            return Exceptions.handle(exception);
        }
    }

    private HandledException handleClosedChannel(ClosedChannelException closedChannelException) {
        return Exceptions.createHandled()
                         .error(closedChannelException)
                         .withSystemErrorMessage("An IO exception occurred (closed channel): %s")
                         .handle();
    }
}
