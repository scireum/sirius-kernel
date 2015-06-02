/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Basic implementation of <tt>StructuredOutput</tt>, taking care of all output independent boilerplate code.
 */
public abstract class AbstractStructuredOutput implements StructuredOutput {

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

        private ElementType type;
        private String name;
        private boolean empty = true;

        protected boolean isEmpty() {
            return empty;
        }

        protected void setEmpty(boolean empty) {
            this.empty = empty;
        }

        protected Element(ElementType type, String name) {
            this.type = type;
            this.name = name;
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

    protected List<Element> nesting = new ArrayList<Element>();

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

    /**
     * Must be implemented by subclasses to generate a property.
     *
     * @param name  the name of the property
     * @param value the value of the property
     */
    protected abstract void writeProperty(String name, Object value);

    @Override
    public StructuredOutput beginObject(String name) {
        startObject(name, (Attribute[]) null);
        if (!nesting.isEmpty()) {
            nesting.get(0).setEmpty(false);
        }
        nesting.add(0, new Element(ElementType.OBJECT, name));
        return this;
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
        private List<Attribute> attributes = new ArrayList<Attribute>();
        private String name;

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
         * Finally creates the tag or object with the given name and attributes.
         */
        public void build() {
            beginObject(name, attributes.toArray(new Attribute[attributes.size()]));
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
            throw new IllegalArgumentException("Invalid result structure. Cannot close result. Objects are still " +
                                               "open" + ".");
        }
    }

    @Override
    public StructuredOutput property(String name, Object data) {
        if (getCurrentType() != ElementType.OBJECT && getCurrentType() != ElementType.ARRAY) {
            throw new IllegalArgumentException("Invalid result structure. Cannot place a property here.");
        }
        writeProperty(name, data);
        nesting.get(0).setEmpty(false);
        return this;
    }
}
