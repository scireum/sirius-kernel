/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import sirius.kernel.SiriusExtension
import sirius.kernel.commons.Wait
import kotlin.test.assertTrue

@Tag("nightly")
@ExtendWith(SiriusExtension::class)
class BackgroundLoopSpec {
    @Test
    fun `BackgroundLoop limits to max frequency`() {
        val currentCounter = FastTestLoop.counter.toInt()
        Wait.seconds(10.0)
        val delta = FastTestLoop.counter.toInt() - currentCounter
        //the background loop executed enough calls (should be 10 but we're a bit tolerant here)
        assertTrue { delta >= 8 }
        //the background loop was limited not to execute too often (should be 10 but we're a bit tolerant here)
        assertTrue { delta <= 12 }
    }

    @Test
    fun `BackgroundLoop executes as fast as possible if loop is slow`() {
        val currentCounter = SlowTestLoop.counter.toInt()
        Wait.seconds(10.0)
        val delta = SlowTestLoop.counter.toInt() - currentCounter
        // the background loop executed enough calls (should be 10 but we're a bit tolerant here)
        assertTrue { delta >= 3 }
        // the background loop was limited not to execute too often (should be 10 but we're a bit tolerant here)
        assertTrue { delta <= 6 }
    }
}
