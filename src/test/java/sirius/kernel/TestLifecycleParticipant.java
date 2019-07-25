/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel;

import sirius.kernel.di.std.Priorized;

/**
 * Invoked before and after all tests have been executed.
 * <p>
 * This can monitor the test progress or fail a test suite if an invalid behaviour has been observed whiled running
 * the tests.
 */
public interface TestLifecycleParticipant extends Priorized {

    /**
     * Invoked before the actual tests are run.
     */
    void beforeTests();

    /**
     * Invoked after all tests have been executed.
     * <p>
     * Throw an exception here to fail the test run.
     */
    void afterTests();
}
