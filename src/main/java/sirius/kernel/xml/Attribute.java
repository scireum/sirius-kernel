/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

/**
 * Used to pass in attributes when creating objects for a {@link StructuredOutput}.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public class Attribute {
    private String name;
    private Object value;

    /**
     * Returns the name of the attribute.
     *
     * @return the name of the attribute
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the attribute.
     *
     * @param name the name of the attribute
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the value of the attribute.
     *
     * @return the value of the attribute
     */
    public Object getValue() {
        return value;
    }

    /**
     * Sets the value of the attribute.
     *
     * @param value the value of the attribute.
     */
    public void setValue(Object value) {
        this.value = value;
    }

    private Attribute(String name, Object value) {
        super();
        this.name = name;
        this.value = value;
    }

    /**
     * Creates a new attribute with the given name and value.
     *
     * @param name  the name of the attribute
     * @param value the value of the attribute
     * @return a new attribute with the given name and value
     */
    public static Attribute set(String name, Object value) {
        return new Attribute(name, value);
    }

}