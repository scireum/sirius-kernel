/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.function.BiConsumer;

/**
 * Interface for writing structured outputs like XML or JSON.
 */
public interface StructuredOutput {

    /**
     * Starts the result with a default root element ("result").
     *
     * @return the output itself for fluent method calls
     */
    StructuredOutput beginResult();

    /**
     * Starts the result by specifying the name of the root element.
     *
     * @param name the name of the root element
     * @return the output itself for fluent method calls
     */
    StructuredOutput beginResult(@Nonnull String name);

    /**
     * Finishes (closes) the result
     */
    void endResult();

    /**
     * Starts a new object with the given name and attributes
     *
     * @param name       the name of the object to create
     * @param attributes the attributes to add to the object
     * @return the output itself for fluent method calls
     */
    StructuredOutput beginObject(@Nonnull String name, Attribute... attributes);

    /**
     * Ends the currently open object.
     *
     * @return the output itself for fluent method calls
     */
    StructuredOutput endObject();

    /**
     * Outputs all record components of the given record as an object with the given name.
     * @param name the name to use for the object
     * @param object the record itself to output. Note that if <tt>object</tt> is <tt>null</tt>, nothing will be output
     * @return the output itself for fluent method calls
     */
    StructuredOutput object(@Nonnull String name, Record object);

    /**
     * Adds a property to the current object.
     *
     * @param name the name of the property
     * @param data the value of the property
     * @return the output itself for fluent method calls
     */
    StructuredOutput property(@Nonnull String name, @Nullable Object data);

    /**
     * Adds a property to the current object.
     * <p>
     * This will create a property with the specified data as value or empty string if the value is null.
     *
     * @param name the name of the property
     * @param data the value of the property
     * @return the output itself for fluent method calls
     */
    StructuredOutput nullsafeProperty(@Nonnull String name, @Nullable Object data);

    /**
     * Starts an array with is added to the current object as "name".
     *
     * @param name the name of the array
     * @return the output itself for fluent method calls
     */
    StructuredOutput beginArray(@Nonnull String name);

    /**
     * Ends the currently open array.
     *
     * @return the output itself for fluent method calls
     */
    StructuredOutput endArray();

    /**
     * Outputs the given collection as array.
     * <p>
     * This will create a property with the given name and the given array as value
     *
     * @param name        the name of the property
     * @param elementName the name used to generate inner elements (if required, e.g. XML)
     * @param array       the array to output
     * @return the output itself for fluent method calls
     */
    StructuredOutput array(@Nonnull String name, @Nonnull String elementName, @Nonnull Collection<?> array);

    /**
     * Outputs the given collection as array while using the given <tt>arrayConsumer</tt> to generate the array
     * contents.
     *
     * @param name          the name of the array property to create
     * @param array         the collection to generate inner elements
     * @param arrayConsumer the consumer which creates the array content per child element in <tt>array</tt>
     * @param <E>           the type of elements in <tt>array</tt>
     * @return the output itself for fluent method calls
     */
    <E> StructuredOutput array(@Nonnull String name,
                               @Nonnull Collection<E> array,
                               BiConsumer<StructuredOutput, E> arrayConsumer);
}
