/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.nls

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import sirius.kernel.SiriusExtension
import sirius.kernel.async.CallContext
import sirius.kernel.commons.Amount
import java.time.*
import kotlin.test.assertEquals

/**
 * Tests the [NLS] class.
 */
@ExtendWith(SiriusExtension::class)

class NLSTest {

    @Test
    fun `toMachineString() formats a LocalDate as date without time`() {
        val date = LocalDate.of(2014, 8, 9)
        val result = NLS.toMachineString(date)
        assertEquals("2014-08-09", result)
    }

    @Test
    fun `toMachineString() formats a LocalDateTime as date with time`() {
        val date = LocalDateTime.of(2014, 8, 9, 12, 0, 59)
        val result = NLS.toMachineString(date)
        assertEquals("2014-08-09 12:00:59", result)
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|', textBlock = """
        123456.789 | 123456.79
        123456.81  | 123456.81
        0.113      | 0.11
        -11111.1   | -11111.10
        1          | 1.00
        -1         | -1.00
        0          | 0.00"""
    )
    fun `toMachineString() of Amount is properly formatted`(
        input: Double,
        output: String
    ) {
        assertEquals(NLS.toMachineString(Amount.of(input)), output)
    }

    @Test
    fun `toUserString() formats a LocalDateTime as date with time`() {
        val date = LocalDateTime.of(2014, 8, 9, 12, 0, 59)
        CallContext.getCurrent().language = "de"
        val result = NLS.toUserString(date)
        assertEquals("09.08.2014 12:00:59", result)
    }

    @Test
    fun `toUserString() formats a LocalTime as simple time`() {
        val date = LocalTime.of(17, 23, 15)
        CallContext.getCurrent().language = "de"
        val result = NLS.toUserString(date)
        assertEquals("17:23:15", result)
    }


    @Test
    fun `toUserString() formats a LocalDate as date without time`() {
        val date = LocalDate.of(2014, 8, 9)
        CallContext.getCurrent().language = "de"
        assertEquals("09.08.2014", NLS.toUserString(date))
    }

    @Test
    fun `toUserString() formats an Instant successfully`() {
        val instant = Instant.now()
        val date = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
        CallContext.getCurrent().language = "de"
        val dateFormatted = NLS.toUserString(date)
        val instantFormatted = NLS.toUserString(instant)
        assertEquals(dateFormatted, instantFormatted)
    }

    @Test
    fun `toUserString() formats null as empty string`() {
        val input = null
        assertEquals("", NLS.toUserString(input))
    }

    @Test
    fun `toSpokenDate() formats dates and dateTimes correctly`() {
        assertEquals("heute", NLS.toSpokenDate(LocalDate.now()))
        assertEquals("vor wenigen Minuten", NLS.toSpokenDate(LocalDateTime.now()))
        assertEquals("gestern", NLS.toSpokenDate(LocalDate.now().minusDays(1)))
        assertEquals("gestern", NLS.toSpokenDate(LocalDateTime.now().minusDays(1)))
        assertEquals("morgen", NLS.toSpokenDate(LocalDate.now().plusDays(1)))
        assertEquals("morgen", NLS.toSpokenDate(LocalDateTime.now().plusDays(1)))
        assertEquals("01.01.2114", NLS.toSpokenDate(LocalDate.of(2114, 1, 1)))
        assertEquals("01.01.2114", NLS.toSpokenDate(LocalDateTime.of(2114, 1, 1, 0, 0)))
        assertEquals("01.01.2014", NLS.toSpokenDate(LocalDate.of(2014, 1, 1)))
        assertEquals("01.01.2014", NLS.toSpokenDate(LocalDateTime.of(2014, 1, 1, 0, 0)))
        assertEquals("vor wenigen Minuten", NLS.toSpokenDate(LocalDateTime.now().minusMinutes(5)))
        assertEquals("vor 35 Minuten", NLS.toSpokenDate(LocalDateTime.now().minusMinutes(35)))
        assertEquals("vor einer Stunde", NLS.toSpokenDate(LocalDateTime.now().minusHours(1)))
        assertEquals("vor 4 Stunden", NLS.toSpokenDate(LocalDateTime.now().minusHours(4)))
        assertEquals("in der nächsten Stunde", NLS.toSpokenDate(LocalDateTime.now().plusMinutes(40)))
        assertEquals("in 3 Stunden", NLS.toSpokenDate(LocalDateTime.now().plusHours(4)))
    }
}