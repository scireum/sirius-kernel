/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons

import sirius.kernel.BaseSpecification
import sirius.kernel.di.std.Part

import java.text.ParseException
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class AdvancedDateParserSpec extends BaseSpecification {

    @Part
    private static TimeProvider timeProvider;

    def "German date can be parsed"() {
        when:
        AdvancedDateParser parser = new AdvancedDateParser("de")
        timeProvider.setClock(Clock.fixed(Instant.parse("2017-07-07T12:34:55.00Z"), ZoneOffset.UTC))
        then:
        parser.parse("07.07.2017").asDateTime() == LocalDateTime.parse("2017-07-07T00:00")
        parser.parse("07.07.").asDateTime() == LocalDateTime.parse("2017-07-07T00:00")
        parser.parse("07.07.17").asDateTime() == LocalDateTime.parse("2017-07-07T00:00")
        parser.parse("heute").asDateTime() == LocalDateTime.parse("2017-07-07T12:34:55")
    }

    def "An ISO date can be parsed"() {
        when:
        AdvancedDateParser parser = new AdvancedDateParser("de")
        then:
        parser.parse("2017-07-07T12:34:00").asDateTime() == LocalDateTime.parse("2017-07-07T12:34")
        parser.parse("2017-07-07 12:34:00").asDateTime() == LocalDateTime.parse("2017-07-07T12:34")
        parser.parse("2017-07-07").asDateTime() == LocalDateTime.parse("2017-07-07T00:00")
        parser.parse("2017-07-07T00:00").asDateTime() == LocalDateTime.parse("2017-07-07T00:00")
        parser.parse("2017-07-07T12:34:56").asDateTime() == LocalDateTime.parse("2017-07-07T12:34:56")
    }

    def "American date format can be parsed"() {
        when:
        AdvancedDateParser parser = new AdvancedDateParser("en")
        timeProvider.setClock(Clock.fixed(Instant.parse("2017-07-07T12:34:55.00Z"), ZoneOffset.UTC))
        then:
        parser.parse("07/07/2017").asDateTime() == LocalDateTime.parse("2017-07-07T00:00")
        parser.parse("07/07").asDateTime() == LocalDateTime.parse("2017-07-07T00:00")
        parser.parse("07/07/17").asDateTime() == LocalDateTime.parse("2017-07-07T00:00")
        parser.parse("today").asDateTime() == LocalDateTime.parse("2017-07-07T12:34:55")
        parser.parse("07/07/17 11:00 am").asDateTime() == LocalDateTime.parse("2017-07-07T11:00:00")
        parser.parse("07/27/17 11:00 am").asDateTime() == LocalDateTime.parse("2017-07-27T11:00:00")
        parser.parse("today 11:00 pm").asDateTime() == LocalDateTime.parse("2017-07-07T23:00:00")
    }

    def "British date format can be parsed"() {
        when:
        AdvancedDateParser parser = new AdvancedDateParser("en", true)
        then:
        parser.parse("27/07/17 11:00 am").asDateTime() == LocalDateTime.parse("2017-07-27T11:00:00")
        when:
        parser.parse("07/27/17 11:00 am").asDateTime()
        then:
        thrown(ParseException)
    }

    def "Relative dates can be parsed"() {
        when:
        AdvancedDateParser parser = new AdvancedDateParser("de")
        timeProvider.setClock(Clock.fixed(Instant.parse("2017-07-07T12:34:55.00Z"), ZoneOffset.UTC))
        then:
        parser.parse("+1").asDateTime() == LocalDateTime.parse("2017-07-08T12:34:55")
        parser.parse("+1 tag").asDateTime() == LocalDateTime.parse("2017-07-08T12:34:55")
        parser.parse("heute - 1 woche").asDateTime() == LocalDateTime.parse("2017-06-30T12:34:55")
        parser.parse("heute + 1 monat").asDateTime() == LocalDateTime.parse("2017-08-07T12:34:55")
    }

    def "Ranges are properly enforced"() {
        given:
        AdvancedDateParser parser = new AdvancedDateParser("de")
        when: "An out of range day of month is used..."
        parser.parse("32.07.2017").asDateTime()
        then:
        thrown(ParseException)
        when: "The number of days exeeds the days in a specific month..."
        parser.parse("31.06.2017").asDateTime()
        then:
        thrown(ParseException)
        when: "The month value exceeds 12..."
        parser.parse("30.13.2017").asDateTime()
        then:
        thrown(ParseException)
        when: "0 is used a day of month..."
        parser.parse("00.12.2017").asDateTime()
        then:
        thrown(ParseException)
        when: "An out of range value is used as hour of day..."
        parser.parse("07.07.2017 25:00:00").asDateTime()
        then:
        thrown(ParseException)
        when: "An out of range value is used as hour of day..."
        parser.parse("07.07.2017 12pm").asDateTime()
        then:
        thrown(ParseException)
        when: "An out of range value is used as hour of day..."
        parser.parse("07.07.2017 12am").asDateTime()
        then:
        thrown(ParseException)
        when: "An out of range value is used as minute of hour..."
        parser.parse("07.07.2017 12:71:00").asDateTime()
        then:
        thrown(ParseException)
        when: "An out of range value is used as second of minute..."
        parser.parse("07.07.2017 12:00:71").asDateTime()
        then:
        thrown(ParseException)
    }


    def "The parser can re-digest its output"() {
        given:
        AdvancedDateParser parser = new AdvancedDateParser("de")
        when: "A date is parsed into a DateSelection"
        def dateSelection = parser.parse("07.07.2017 12:34:56");
        then: "its result can be re-parsed by the date parser..."
        parser.parse(dateSelection.toString()).asDateTime() == dateSelection.asDateTime()
    }


}
