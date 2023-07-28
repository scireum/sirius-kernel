/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import sirius.kernel.SiriusExtension
import java.util.logging.Level
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests the [Exceptions] class.
 */
@ExtendWith(SiriusExtension::class)
internal class ExceptionsTest {
    @Test
    fun `Warning is logged when a deprecated method is called`() {
        LogHelper.clearMessages()
        caller()
        assertTrue {
            LogHelper.hasMessage(
                    Level.WARNING,
                    Exceptions.DEPRECATION_LOG,
                    "^The deprecated method 'sirius.kernel.health.ExceptionsTest.deprecatedMethod'"
                            + " was called by 'sirius.kernel.health.ExceptionsTest.caller'"
            )
        }
    }

    private fun caller() {
        deprecatedMethod()
    }

    private fun deprecatedMethod() {
        Exceptions.logDeprecatedMethodUse()
    }

    @Test
    fun `root cause remains when exception is handled`() {
        val root = Exceptions.createHandled().withSystemErrorMessage("Root Cause").handle()
        val exception = Exceptions.handle(Exception(IllegalArgumentException(root)))
        assertEquals(root, Exceptions.getRootCause(exception))
    }
}
