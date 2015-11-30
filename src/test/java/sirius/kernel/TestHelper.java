/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel;

import sirius.kernel.nls.NLS;

/**
 * Initializes and stops Sirius as part of the tests.
 */
public class TestHelper {

    private static Class<?> frameworkStarter = null;

    private TestHelper() {
    }

    /**
     * Initializes the framework for the given class.
     * <p>
     * The given class is used to determine when to stop the framework again.
     * So if a single test is executed, it will start and stop Sirius. However, if
     * a test suite is executed, it will start the framework once for all tests and
     * terminate it afterwards.
     *
     * @param testClass the test class starting the framework
     */
    public static void setUp(Class<?> testClass) {
        if (frameworkStarter == null) {
            frameworkStarter = testClass;
            Sirius.start(new Setup(Setup.Mode.TEST, Sirius.class.getClassLoader()));
            NLS.setDefaultLanguage("de");
        }
    }

    /**
     * Terminates the framework, if the given class did originally start Sirius.
     *
     * @param testClass the test class being finished
     */
    public static void tearDown(Class<?> testClass) {
        if (frameworkStarter == testClass) {
            Sirius.stop();
            NLS.getTranslationEngine().reportMissingTranslations();
        }
    }
}
