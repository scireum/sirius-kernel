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

/**
 * Interface for writing structured outputs like XML or JSON.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public interface StructuredOutput {

    /**
     * Starts the result
     */
    void beginResult();

    /**
     * Starts the result by specifying the name of the root element.
     *
     * @param name the name of the root element
     */
    void beginResult(@Nonnull String name);

    /**
     * Finishes (closes) the result
     */
    void endResult();

    /**
     * Starts a new object with the given name.
     *
     * @param name the name of the element to start
     */
    void beginObject(@Nonnull String name);

    /**
     * Starts a new object with the given name and attributes
     *
     * @param name       the name of the object to create
     * @param attributes the attributes to add to the object
     */
    void beginObject(@Nonnull String name, Attribute... attributes);

    /**
     * Ends the currently open object.
     */
    void endObject();

    /**
     * Adds a property to the current object.
     *
     * @param name the name of the property
     * @param data the value of the property
     */
    void property(@Nonnull String name, @Nullable Object data);

    /**
     * Starts an array with is added to the current object as "name".
     *
     * @param name the name of the array
     */
    void beginArray(@Nonnull String name);

    /**
     * Ends the currently open array.
     */
    void endArray();

    /**
     * Outputs the given collection as array.
     * <p>This will create a property with the given name and the given array as value</p>
     *
     * @param name        the name of the property
     * @param elementName the name used to generate inner elements (if required, e.g. XML)
     * @param array       the array to output
     */
    void array(@Nonnull String name, @Nonnull String elementName, @Nonnull Collection<?> array);

}
