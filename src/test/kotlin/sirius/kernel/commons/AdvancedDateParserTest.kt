/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import sirius.kernel.SiriusExtension
import sirius.kernel.di.std.Part

import java.text.ParseException
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.assertEquals

/**
 * Tests the [AdvancedDateParser] class.
 */
@ExtendWith(SiriusExtension::class)
class AdvancedDateParserTest {

    @Test
    fun `German date can be parsed`() {
        val parser = AdvancedDateParser("de")
        timeProvider.setClock(Clock.fixed(Instant.parse("2017-07-07T12:34:55.00Z"), ZoneOffset.UTC))

        assertEquals(LocalDateTime.parse("2017-07-07T00:00"), parser.parse("07.07.2017").asDateTime())
        assertEquals(LocalDateTime.parse("2017-07-07T00:00"), parser.parse("07.07.").asDateTime())
        assertEquals(LocalDateTime.parse("2017-07-07T00:00"), parser.parse("07.07.17").asDateTime())
        assertEquals(LocalDateTime.parse("2017-07-07T12:34:55"), parser.parse("heute").asDateTime())
    }

    @Test
    fun `An ISO date can be parsed`() {
        val parser = AdvancedDateParser("de")

        assertEquals(LocalDateTime.parse("2017-07-07T12:34"), parser.parse("2017-07-07T12:34:00").asDateTime())
        assertEquals(LocalDateTime.parse("2017-07-07T12:34"), parser.parse("2017-07-07 12:34:00").asDateTime())
        assertEquals(LocalDateTime.parse("2017-07-07T00:00"), parser.parse("2017-07-07").asDateTime())
        assertEquals(LocalDateTime.parse("2017-07-07T00:00"), parser.parse("2017-07-07T00:00").asDateTime())
        assertEquals(LocalDateTime.parse("2017-07-07T12:34:56"), parser.parse("2017-07-07T12:34:56").asDateTime())
    }

    @Test
    fun `American date format can be parsed`() {
        val parser = AdvancedDateParser("en")
        timeProvider.setClock(Clock.fixed(Instant.parse("2017-07-07T12:34:55.00Z"), ZoneOffset.UTC))

        assertEquals(LocalDateTime.parse("2017-07-07T00:00"), parser.parse("07/07/2017").asDateTime())
        assertEquals(LocalDateTime.parse("2017-07-07T00:00"), parser.parse("07/07").asDateTime())
        assertEquals(LocalDateTime.parse("2017-07-07T00:00"), parser.parse("07/07/17").asDateTime())
        assertEquals(LocalDateTime.parse("2017-07-07T12:34:55"), parser.parse("today").asDateTime())
        assertEquals(LocalDateTime.parse("2017-07-07T11:00:00"), parser.parse("07/07/17 11:00 am").asDateTime())
        assertEquals(LocalDateTime.parse("2017-07-27T11:00:00"), parser.parse("07/27/17 11:00 am").asDateTime())
        assertEquals(LocalDateTime.parse("2017-07-07T23:00:00"), parser.parse("today 11:00 pm").asDateTime())
    }

    @Test
    fun `British date format can be parsed`() {
        val parser = AdvancedDateParser("en", true)

        assertEquals(LocalDateTime.parse("2017-07-27T11:00:00"), parser.parse("27/07/17 11:00 am").asDateTime())

        assertThrows<ParseException> {
            parser.parse("07/27/17 11:00 am").asDateTime()
        }
    }

    @Test
    fun `French date formats can be parsed`() {
        val parser = AdvancedDateParser("fr", true)

        assertEquals(LocalDateTime.parse("2017-07-27T00:00:00"), parser.parse("27.07.17").asDateTime())
        assertEquals(LocalDateTime.parse("2017-07-27T00:00:00"), parser.parse("27/07/17").asDateTime())

        assertThrows<ParseException> {
            parser.parse("07/27/17").asDateTime()
        }
    }

    @Test
    fun `Relative dates can be parsed`() {
        val parser = AdvancedDateParser("de")
        timeProvider.setClock(Clock.fixed(Instant.parse("2017-07-07T12:34:55.00Z"), ZoneOffset.UTC))

        assertEquals(LocalDateTime.parse("2017-07-08T12:34:55"), parser.parse("+1").asDateTime())
        assertEquals(LocalDateTime.parse("2017-07-08T12:34:55"), parser.parse("+1 tag").asDateTime())
        assertEquals(LocalDateTime.parse("2017-06-30T12:34:55"), parser.parse("heute - 1 woche").asDateTime())
        assertEquals(LocalDateTime.parse("2017-08-07T12:34:55"), parser.parse("heute + 1 monat").asDateTime())
    }

    @Test
    fun `Ranges are properly enforced`() {
        val parser = AdvancedDateParser("de")

        // An out-of-range day of month is used...
        assertThrows<ParseException> {
            parser.parse("32.07.2017").asDateTime()
        }
        // The number of days exceeds the days in a specific month...
        assertThrows<ParseException> {
            parser.parse("31.06.2017").asDateTime()
        }
        // The month value exceeds 12...
        assertThrows<ParseException> {
            parser.parse("30.13.2017").asDateTime()
        }
        // 0 is used a day of month...
        assertThrows<ParseException> {
            parser.parse("00.12.2017").asDateTime()
        }
        // An out-of-range value is used as hour of day...
        assertThrows<ParseException> {
            parser.parse("07.07.2017 25:00:00").asDateTime()
        }
        // An out-of-range value is used as hour of day...
        assertThrows<ParseException> {
            parser.parse("07.07.2017 12pm").asDateTime()
        }
        // An out-of-range value is used as hour of day...
        assertThrows<ParseException> {
            parser.parse("07.07.2017 12am").asDateTime()
        }
        // An out-of-range value is used as minute of hour...
        assertThrows<ParseException> {
            parser.parse("07.07.2017 12:71:00").asDateTime()
        }
        // An out-of-range value is used as second of minute...
        assertThrows<ParseException> {
            parser.parse("07.07.2017 12:00:71").asDateTime()
        }
    }

    @Test
    fun `The parser can re-digest its output`() {
        val parser = AdvancedDateParser("de")
        // A date is parsed into a DateSelection
        val dateSelection = parser.parse("07.07.2017 12:34:56")
        // Its result can be re-parsed by the date parser...
        assertEquals(dateSelection.asDateTime(), parser.parse(dateSelection.toString()).asDateTime())
    }

    companion object {
        @Part
        @JvmStatic
        private lateinit var timeProvider: TimeProvider
    }
}
