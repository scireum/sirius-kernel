/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.timer

import sirius.kernel.BaseSpecification
import sirius.kernel.async.Future
import sirius.kernel.commons.TimeProvider
import sirius.kernel.commons.Wait
import sirius.kernel.di.std.Part

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class EveryDayTaskSpec extends BaseSpecification {

    @Part
    private static TimeProvider timeProvider

    @Part
    private static EndOfDayTaskExecutor endOfDayTaskExecutor

    @Part
    private static Timers timers

    def "EveryDayTask is executed"() {
        when: "We pretent that it is the start hour to execute all tasks..."
        timeProvider.setClock(Clock.fixed(Instant.parse("2020-02-20T22:02:42.00Z"), ZoneId.systemDefault()))
        and:
        timers.runEveryDayTimers(22)
        and:
        EndOfDayTestTask.executed.await(Duration.ofSeconds(30))
        then:
        EndOfDayTestTask.executed.isSuccessful()
        cleanup:
        timeProvider.setClock(Clock.systemDefaultZone())
    }

    def "EveryDayTask is not executed if timeout is reached"() {
        setup:
        EndOfDayTestTask.executed = new Future()
        when: "We set an artificial clock in the morning where no tasks should run anymore..."
        timeProvider.setClock(Clock.fixed(Instant.parse("2020-02-20T06:02:42.00Z"), ZoneId.systemDefault()))
        and: "We pretend it to be the start hour of end of day tasks..."
        timers.runEveryDayTimers(22)
        and:
        Wait.seconds(2)
        then:
        !EndOfDayTestTask.executed.isSuccessful()
        cleanup:
        timeProvider.setClock(Clock.systemDefaultZone())
    }

}
