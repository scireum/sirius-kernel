/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.timer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import sirius.kernel.SiriusExtension
import sirius.kernel.async.Future
import sirius.kernel.commons.TimeProvider
import sirius.kernel.commons.Wait
import sirius.kernel.di.std.Part
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(SiriusExtension::class)
class EveryDayTaskTest {

    @Test
    fun `EveryDayTask is executed`() {
        // We pretend that it is the start hour to execute all tasks...
        timeProvider.setClock(Clock.fixed(Instant.parse("2020-02-20T22:02:42.00Z"), ZoneId.systemDefault()))
        timers.runEveryDayTimers(22)
        EndOfDayTestTask.executed.await(Duration.ofSeconds(30))

        assertTrue { EndOfDayTestTask.executed.isSuccessful }

        // Reset the clock to the current time...
        timeProvider.setClock(Clock.systemDefaultZone())
    }

    @Test
    fun `EveryDayTask is not executed if timeout is reached`() {
        EndOfDayTestTask.executed = Future()
        // We set an artificial clock in the morning where no tasks should run anymore...
        timeProvider.setClock(Clock.fixed(Instant.parse("2020-02-20T06:02:42.00Z"), ZoneId.systemDefault()))
        // We pretend it to be the start hour of end of day tasks...
        timers.runEveryDayTimers(22)
        Wait.seconds(2.0)
        assertFalse { EndOfDayTestTask.executed.isSuccessful }

        // Reset the clock to the current time...
        timeProvider.setClock(Clock.systemDefaultZone())
    }

    companion object {
        @Part
        @JvmStatic
        private lateinit var timeProvider: TimeProvider

        @Part
        @JvmStatic
        private lateinit var endOfDayTaskExecutor: EndOfDayTaskExecutor

        @Part
        @JvmStatic
        private lateinit var timers: Timers
    }
}
