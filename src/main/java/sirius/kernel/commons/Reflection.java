/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import sirius.kernel.health.Exceptions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Helper class for generic reflection tasks.
 */
public class Reflection {

    private Reflection() {
    }

    /**
     * Converts the first character of a given string to upper case.
     * <p>
     * Comes in handy when translating properties to getter/setter names.
     *
     * @param string the name of the property to convert
     * @return the given string, with an upper-case letter at the start or <tt>null</tt> if the input was null
     */
    @Nullable
    public static String toFirstUpper(@Nullable String string) {
        if (Strings.isEmpty(string)) {
            return string;
        }
        if (string.length() == 1) {
            return string.toUpperCase();
        }
        return string.substring(0, 1).toUpperCase() + string.substring(1);
    }

    /**
     * Returns the getter method according to the java beans specification for a
     * given property.
     *
     * @param clazz    the class in which the method should be searched
     * @param property the name of the property for which the getter is requested
     * @return the <tt>Method</tt> which is used to get the value
     * @throws IllegalArgumentException if the getter cannot be obtained
     */
    @Nonnull
    @SuppressWarnings("squid:S1141")
    @Explain("A nested catch block seems to be the most readable solution here")
    public static Method getter(@Nonnull Class<?> clazz, @Nonnull String property) {
        try {
            try {
                return clazz.getMethod("get" + toFirstUpper(property));
            } catch (NoSuchMethodException e) {
                Exceptions.ignore(e);
                return getterAsIs(clazz, property);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(Strings.apply("get-Method for Field %s not found: %s",
                                                             property,
                                                             e.getMessage()), e);
        }
    }

    private static Method getterAsIs(@Nonnull Class<?> clazz, @Nonnull String property) throws NoSuchMethodException {
        try {
            return clazz.getMethod("is" + toFirstUpper(property));
        } catch (NoSuchMethodException ex) {
            Exceptions.ignore(ex);
            return clazz.getMethod(property);
        }
    }

    /**
     * Returns the setter method according to the java beans specification for a
     * given property.
     *
     * @param clazz     the class in which the method should be searched
     * @param property  the name of the property for which the setter is requested
     * @param fieldType determines the type of the field (which is accepted as parameter of the setter)
     * @return the <tt>Method</tt> which is used to get the value
     * @throws IllegalArgumentException if the setter cannot be obtained
     */
    public static Method setter(Class<?> clazz, String property, Class<?> fieldType) {
        try {
            return clazz.getMethod("set" + toFirstUpper(property), fieldType);
        } catch (Exception e) {
            throw new IllegalArgumentException(Strings.apply("set-Method for Field %s not found: %s",
                                                             property,
                                                             e.getMessage()), e);
        }
    }

    /**
     * Evaluates the given access path (dot separated getters) and returns the result.
     * <p>
     * An access path can look like <tt>foo.bar.baz</tt> and represents: {@code root.getFoo().getBar().getBaz()}.
     * If any of the getters returns <tt>null</tt>, <tt>null</tt> will also be the result of the evaluation.
     *
     * @param path the access path to evaluate
     * @param root the root object on which the first getter is called
     * @return the result of the last getter
     */
    public static Object evalAccessPath(String path, Object root) {
        if (root == null) {
            return null;
        }
        if (Strings.isEmpty(path)) {
            return root;
        }
        Tuple<String, String> pair = Strings.split(path, ".");
        Method m = getter(root.getClass(), pair.getFirst());
        try {
            return evalAccessPath(pair.getSecond(), m.invoke(root));
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(Strings.apply("Cannot invoke %s on %s (%s)",
                                                             m.getName(),
                                                             root,
                                                             m.getDeclaringClass().getName()), e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(Strings.apply("Error invoking %s on %s (%s): %s (%s)",
                                                             m.getName(),
                                                             root,
                                                             m.getDeclaringClass().getName(),
                                                             e.getCause().getMessage(),
                                                             e.getCause().getClass().getName()), e);
        }
    }

    /**
     * Calls the given handler for each superclass of the given one.
     * <p>
     * Calls the handler for the given class and each of its superclasses until {@code Object} is reached.
     *
     * @param clazz   the class to start from
     * @param handler the handler to call for each superclass
     * @throws Exception thrown if the handler throws an exception
     */
    public static void walkHierarchy(@Nonnull Class<?> clazz, @Nonnull Callback<Class<?>> handler) throws Exception {
        Class<?> current = clazz;
        while (current != null && !Object.class.equals(current)) {
            handler.invoke(current);
            current = current.getSuperclass();
        }
    }

    /**
     * Returns all fields defined by this class or one of its superclasses. This includes public, protected and
     * private fields.
     *
     * @param clazz the class to collect the fields for
     * @return a list of all defined fields of the given class and its superclasses (excluding {@code Object})
     */
    @Nonnull
    public static List<Field> getAllFields(@Nonnull Class<?> clazz) {
        DataCollector<Field> collector = new DataCollector<>();
        try {
            walkHierarchy(clazz, value -> collector.addAll(Arrays.asList(value.getDeclaredFields())));
        } catch (Exception e) {
            Exceptions.handle(e);
        }
        return collector.getData();
    }

    /**
     * Determines if the given <tt>superclass</tt> is the same or a superclass or superinterface of the given
     * <tt>classInQuestion</tt>.
     * <p>
     * This is essentially a shortcut for {@code superclass.isAssignableFrom(classInQuestion)} which seems
     * to be more natural.
     *
     * @param superclass      the designated superclass or superinterface
     * @param classInQuestion the class in question
     * @return <tt>true</tt> if the class in question is the same, a subclass or subinterface of the given
     * <tt>superclass</tt>.
     */
    public static boolean isSubclassOf(@Nonnull Class<?> superclass, @Nonnull Class<?> classInQuestion) {
        return superclass.isAssignableFrom(classInQuestion);
    }
}
