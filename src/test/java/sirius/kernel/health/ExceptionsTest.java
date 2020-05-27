/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import sirius.kernel.TestHelper;

import java.util.logging.Level;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the {@link Exceptions} class.
 */
public class ExceptionsTest {

    @BeforeClass
    public static void setUp() {
        TestHelper.setUp(ExceptionsTest.class);
    }

    @AfterClass
    public static void tearDown() {
        TestHelper.tearDown(ExceptionsTest.class);
    }

    @Test
    public void testDeprecatedMethodCallWarner() {
        LogHelper.clearMessages();
        caller();
        assertTrue(LogHelper.hasMessage(Level.WARNING,
                                        Exceptions.DEPRECATION_LOG,
                                        "^The deprecated method 'sirius.kernel.health.ExceptionsTest.deprecatedMethod'"
                                        + " was called by 'sirius.kernel.health.ExceptionsTest.caller'"));
    }

    private void caller() {
        deprecatedMethod();
    }

    private void deprecatedMethod() {
        Exceptions.logDeprecatedMethodUse();
    }

    @Test
    public void testRootCauseRemains() {
        HandledException root = Exceptions.createHandled().withSystemErrorMessage("Root Cause").handle();
        HandledException ex = Exceptions.handle(new Exception(new IllegalArgumentException(root)));
        assertEquals(root, Exceptions.getRootCause(ex));
    }

}
