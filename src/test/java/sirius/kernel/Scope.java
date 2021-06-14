/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel;

import org.spockframework.runtime.extension.ExtensionAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Permits to control the execution of a test class or spec.
 * <p>
 * This can be placed on a test class or {@link BaseSpecification spec} to control the execution of it.
 * <p>
 * Some tests are quite time consuming and do not need to be executed for every build. These can be
 * annotated with a scope and then might be only executed once every night.
 * <p>
 * If a scope value is present, the test class or spec is only executed if the system property <tt>test.SCOPE</tt>
 * is <tt>true</tt>. So to run nightly tests, one has to pass <tt>-Dtest.nightly=true</tt>.
 * <p>
 * Note that the scope isn't checked if the test class is run manually in the IDE which is also quite useful.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtensionAnnotation(ScopeExtension.class)
public @interface Scope {

    /**
     * Provides a commonly used scope which is used to mark test that should only be run once per night and not
     * on every build.
     */
    String SCOPE_NIGHTLY = "nightly";

    /**
     * Returns the scope value to apply.
     *
     * @return the scope of this test class or spec
     */
    String value() default "";
}
