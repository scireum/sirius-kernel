/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.*

/**
 * Tests the [Dates] class.
 */
class DatesTest {
    @Test
    fun computeLatestDateTime() {
        val theDayBeforeYesterday = LocalDateTime.now().minusDays(2)
        val yesterday = LocalDateTime.now().minusDays(1)
        val now = LocalDateTime.now()
        assertEquals(now, Dates.computeLatestDateTime(null, theDayBeforeYesterday, now, yesterday))
        assertEquals(null, null)
    }

    @Test
    fun computeLatestDate() {
        val theDayBeforeYesterday = LocalDate.now().minusDays(2)
        val yesterday = LocalDate.now().minusDays(1)
        val now = LocalDate.now()
        assertEquals(now, Dates.computeLatestDate(null, theDayBeforeYesterday, now, yesterday))
        assertEquals(null, null)
    }

    @Test
    fun computeEarliestDateTime() {
        val theDayBeforeYesterday = LocalDateTime.now().minusDays(2)
        val yesterday = LocalDateTime.now().minusDays(1)
        val now = LocalDateTime.now()
        assertEquals(theDayBeforeYesterday, Dates.computeEarliestDateTime(null, theDayBeforeYesterday, now, yesterday))
        assertEquals(null, null)
    }

    @Test
    fun computeEarliestDate() {
        val theDayBeforeYesterday = LocalDate.now().minusDays(2)
        val yesterday = LocalDate.now().minusDays(1)
        val now = LocalDate.now()
        assertEquals(theDayBeforeYesterday, Dates.computeEarliestDate(null, theDayBeforeYesterday, now, yesterday))
        assertEquals(null, null)
    }
}
