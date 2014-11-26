package sirius.kernel.di.std;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a given class as "self registering".
 * <p>
 * If a non empty name is given, the part is registered with the given name and
 * for the given classes. Otherwise, the part is directly registered without any
 * unique name.
 * <p>
 * If no <tt>classes</tt> are given, the class is registered for its own class, and all implemented interfaces. This
 * is probably the right choice in many situations, therefore this annotation can be used without any parameters in
 * most cases.
 * <p>
 * Classes wearing this annotations must have a zero args constructor.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Register {

    /**
     * Unique name of the part.
     *
     * @return the unique name of the part
     */
    String name() default "";

    /**
     * Determines the framework this part belongs to. If a non empty string is given, the part is only registered, if
     * {@link sirius.kernel.Sirius#isFrameworkEnabled(String)} returns <tt>true</tt> for the given framework.
     *
     * @return the name of the framework which has to be enabled in order for this annotation to become active
     */
    String framework() default "";

    /**
     * Classes for which the part is registered.
     *
     * @return the classes for which the part is registered. If this list is empty,
     * all implemented interfaces are used.
     */
    Class<?>[] classes() default {};
}
