/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import sirius.kernel.SiriusExtension
import sirius.kernel.health.HandledException
import sirius.kernel.health.LogHelper
import java.net.URI
import java.util.logging.Level
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the [SOAPClient] class.
 */
@ExtendWith(SiriusExtension::class)
internal class SOAPClientTest {

    @Test
    fun `Unsuccessful SOAPClient calls getting blacklisted get logged only once`() {
        // Use a non-existing endpoint to provoke an exception
        val soapClient = SOAPClient(null, URI.create("http://localhost:2345").toURL())

        // First call with an expected, non blacklist exception
        assertThrows<HandledException> { soapClient.call("action") {} }

        // Second call, the first call being blacklisted with a blacklist message being logged
        LogHelper.clearMessages()
        assertThrows<HandledException> { soapClient.call("action") {} }
        assertTrue { hasBlacklistLogMessage() }

        // On follow-up calls, no blacklisting messages must be logged
        LogHelper.clearMessages()
        assertThrows<HandledException> { soapClient.call("action") {} }
        assertFalse { hasBlacklistLogMessage() }
    }

    private fun hasBlacklistLogMessage(): Boolean {
        return LogHelper.hasMessage(Level.SEVERE, SOAPClient.LOG, ".*blacklist identifier localhost.*")
    }

}
