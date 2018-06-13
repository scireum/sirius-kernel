/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describes an alternative execution scenario for all tests.
 * <p>
 * Applying this annotation to the main <tt>TestSuite</tt> which must be
 * executed using {@link ScenarioSuite}, another test run is created.
 * <p>
 * This run will enhance the system configuration by the file given in
 * {@link #file()}. Additionally the tests to be executed can be filtered
 * using a regex in {@link #includes()} and {@link #excludes()}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Scenarios.class)
public @interface Scenario {

    /**
     * Contains the name of the configuration file to load.
     * <p>
     * All settings provided here will overwrite those in <tt>test.conf</tt> and
     * also the system configuration.
     *
     * @return the scenario configuration
     */
    String file();

    /**
     * Contains a regular expression which filters the tests (fully qualified class names)
     * to be executed.
     *
     * @return the filter regex or an empty string to execute all classes
     */
    String includes() default "";

    /**
     * Contains a regular expression which filters the tests (fully qualified class names)
     * to be ignored and therefore not executed.
     *
     * @return the filter regex or an empty string to execute all classes
     */
    String excludes() default "";
}
