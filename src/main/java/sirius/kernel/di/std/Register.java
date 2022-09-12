/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.std;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a given class as "self registering".
 * <p>
 * If a non empty name is given, the part is registered with the given name and
 * for the given classes. Otherwise, the part is directly registered without any
 * unique name. Note that extending {@link Named} is the preferred way of providing
 * a name. This name can be later used to pick a distinct part via
 * {@link sirius.kernel.di.GlobalContext#getPart(String, Class)}.
 * <p>
 * If no <tt>classes</tt> are given, the class is registered for all classes and interfaces declared or inherited
 * by the annotated class, as long as these are marked with {@link AutoRegister}. This is probably the right choice in
 * many situations, therefore this annotation can and should be used without any parameters in most cases.
 * <p>
 * Additionally the <tt>framework</tt> can be used to only process this annotation if the given framework
 * is enabled - just like {@link Framework} does.
 * <p>
 * Classes wearing this annotations must have a zero args constructor.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
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
     * (which should be the common case), all classes and interfaces marked with {@link AutoRegister}
     * will be used.
     */
    Class<?>[] classes() default {};
}
