/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel;

import sirius.kernel.di.Injector;
import sirius.kernel.nls.NLS;

import java.util.ArrayList;
import java.util.List;

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

            Injector.context()
                    .getPriorizedParts(TestLifecycleParticipant.class)
                    .forEach(TestLifecycleParticipant::beforeTests);
        }
    }

    /**
     * Terminates the framework, if the given class did originally start Sirius.
     *
     * @param testClass the test class being finished
     */
    public static void tearDown(Class<?> testClass) {
        if (frameworkStarter == testClass) {
            performTearDown();
        }
    }

    /**
     /**
      * Performs the framework termination. This can be used from test extensions before a fresh framework instance
      * will be started.
     * <p>
     * Note that this reports <b>all</b> failing {@link TestLifecycleParticipant participants} instead of stopping at
     * the first one, as a single build usually wants to learn about every problem at once. The framework is stopped
     * in any case, even if a participant fails.
     *
     * @throws RuntimeException if one or more {@link TestLifecycleParticipant participants} failed. Additional
     *                          failures are attached as suppressed exceptions.
     */
    public static void performTearDown() {
        if (frameworkStarter == null) {
            // The framework was never started (e.g. a test run without any test requiring it)...
            return;
        }

        try {
            List<RuntimeException> failures = new ArrayList<>();
            for (TestLifecycleParticipant participant : Injector.context()
                                                                .getPriorizedParts(TestLifecycleParticipant.class)) {
                try {
                    participant.afterTests();
                } catch (RuntimeException failure) {
                    failures.add(failure);
                }
            }

            if (!failures.isEmpty()) {
                RuntimeException firstFailure = failures.getFirst();
                failures.stream().skip(1).forEach(firstFailure::addSuppressed);
                throw firstFailure;
            }
        } finally {
            Sirius.stop();
            frameworkStarter = null;
        }
    }
}
