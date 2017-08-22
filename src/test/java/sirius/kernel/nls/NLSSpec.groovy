/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.nls

import sirius.kernel.BaseSpecification
import sirius.kernel.async.CallContext
import sirius.kernel.commons.Amount

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class NLSSpec extends BaseSpecification {

    def "toMachineString() formats a LocalDate as date without time"() {
        given:
        def date = LocalDate.of(2014, 8, 9)
        when:
        def result = NLS.toMachineString(date)
        then:
        result == "2014-08-09"
    }

    def "toMachineString() formats a LocalDateTime as date with time"() {
        given:
        def date = LocalDateTime.of(2014, 8, 9, 12, 00, 59)
        when:
        def result = NLS.toMachineString(date)
        then:
        result == "2014-08-09 12:00:59"
    }

    def "toUserString() formats a LocalDateTime as date with time"() {
        given:
        def date = LocalDateTime.of(2014, 8, 9, 12, 00, 59)
        and:
        CallContext.getCurrent().setLang("de")
        when:
        def result = NLS.toUserString(date)
        then:
        result == "09.08.2014 12:00:59"
    }

    def "toUserString() formats a LocalTime as simple time"() {
        given:
        def date = LocalTime.of(17, 23, 15)
        and:
        CallContext.getCurrent().setLang("de")
        when:
        def result = NLS.toUserString(date)
        then:
        result == "17:23:15"
    }

    def "toUserString() formats a LocalDate as date without time"() {
        given:
        def date = LocalDate.of(2014, 8, 9)
        and:
        CallContext.getCurrent().setLang("de")
        when:
        def result = NLS.toUserString(date)
        then:
        result == "09.08.2014"
    }

    def "toUserString() formats an Instant successfully"() {
        given:
        def date = LocalDateTime.now()
        def instant = Instant.now()
        and:
        CallContext.getCurrent().setLang("de")
        when:
        def dateFormatted = NLS.toUserString(date)
        def instantFormatted = NLS.toUserString(instant)
        then:
        dateFormatted == instantFormatted
    }

    def "toUserString() formats null as empty string"() {
        given:
        def input = null
        when:
        def result = NLS.toUserString(input)
        then:
        result == ""
    }

    def "toUserString() formats an Integer correctly"() {
        given:
        def input = 1234567
        when:
        def result = NLS.toUserString(input)
        then:
        result == "1.234.567"
    }

    def "toUserString() formats a Long correctly"() {
        given:
        def input = 2147483649
        when:
        def result = NLS.toUserString(input)
        then:
        result == "2.147.483.649"
    }

    def "toSpokenDate() formats today as today"() {
        given:
        def date = LocalDate.now()
        when:
        def result = NLS.toSpokenDate(date)
        then:
        result == "heute"
    }

    def "toSpokenDate() formats yesterday as yesterday"() {
        given:
        def date = LocalDate.now().minusDays(1)
        def dateTime = LocalDateTime.now().minusDays(1)
        when:
        def result = NLS.toSpokenDate(date)
        def resultTime = NLS.toSpokenDate(dateTime)
        then:
        result == "gestern"
        resultTime == "gestern"
    }

    def "toSpokenDate() formats tomorrow as tomorrow"() {
        given:
        def date = LocalDate.now().plusDays(1)
        def dateTime = LocalDate.now().plusDays(1)
        when:
        def result = NLS.toSpokenDate(date)
        def resultTime = NLS.toSpokenDate(dateTime)
        then:
        result == "morgen"
        resultTime == "morgen"
    }

    def "toSpokenDate() formats a date in the past as date"() {
        given:
        def date = LocalDate.of(2014, 1, 1)
        when:
        def result = NLS.toSpokenDate(date)
        then:
        result == "01.01.2014"
    }

    def "toSpokenDate() formats a date in the future as date"() {
        given:
        def date = LocalDate.of(2114, 1, 1)
        when:
        def result = NLS.toSpokenDate(date)
        then:
        result == "01.01.2114"
    }

    def "toSpokenDate() formats a a date time of today correctly"() {
        when:
        def someMinutesAgo = LocalDateTime.now().minusMinutes(5)
        def moreThan30MinAgo = LocalDateTime.now().minusMinutes(35)
        def oneHourAgo = LocalDateTime.now().minusHours(1)
        def someHoursAgo = LocalDateTime.now().minusHours(4)
        then:
        NLS.toSpokenDate(someMinutesAgo) == "vor wenigen Minuten"
        NLS.toSpokenDate(moreThan30MinAgo) == "vor 35 Minuten"
        NLS.toSpokenDate(oneHourAgo) == "vor einer Stunde"
        NLS.toSpokenDate(someHoursAgo) == "vor 4 Stunden"
    }

    def "parseUserString with LocalTime parses 9:00 and 9:00:23"() {
        when:
        def input = "9:00"
        def inputWithSeconds = "9:00:23"
        then:
        NLS.parseUserString(LocalTime.class, input).getHour() == 9
        NLS.parseUserString(LocalTime.class, inputWithSeconds).getHour() == 9
    }

    def "parseUserString for an Amount works"() {
        when:
        def input = "34,54"
        then:
        NLS.parseUserString(Amount.class, input).toString() == "34,54"
    }

    def "parseUserString converts different Strings to correct values"() {
        expect:
        NLS.parseUserString(clazz, string) == output
        where:
        clazz         | string         | output
        int.class     | "50.000"       | 50000
        Integer.class | "50.000"       | 50000
        long.class    | "50.000"       | 50000
        Long.class    | "50.000"       | 50000
        Double.class  | "50.000"       | 50000
        double.class  | "50.000"       | 50000
        Float.class   | "50.000"       | 50000
        float.class   | "50.000"       | 50000

        int.class     | "5.12"         | 5
        Integer.class | "5.12"         | 5
        long.class    | "5.12"         | 5
        Long.class    | "5.12"         | 5
        Double.class  | "5.12"         | 5.12
        double.class  | "5.12"         | 5.12
        Float.class   | "5.12"         | 5.12f
        float.class   | "5.12"         | 5.12f

        int.class     | "5,12"         | 5
        Integer.class | "5,12"         | 5
        long.class    | "5,12"         | 5
        Long.class    | "5,12"         | 5
        Double.class  | "5,12"         | 5.12
        double.class  | "5,12"         | 5.12
        Float.class   | "5,12"         | 5.12f
        float.class   | "5,12"         | 5.12f

        int.class     | "3.454.555"    | 3454555
        Integer.class | "3.454.555"    | 3454555
        long.class    | "3.454.555"    | 3454555
        Long.class    | "3.454.555"    | 3454555
        Double.class  | "3.454.555"    | 3454555
        double.class  | "3.454.555"    | 3454555
        Float.class   | "3.454.555"    | 3454555
        float.class   | "3.454.555"    | 3454555

        int.class     | "1.337.001,87" | 1337001
        Integer.class | "1.337.001,87" | 1337001
        long.class    | "1.337.001,87" | 1337001
        Long.class    | "1.337.001,87" | 1337001
        Double.class  | "1.337.001,87" | 1337001.87
        double.class  | "1.337.001,87" | 1337001.87
        //at this point, float loses some precision
        Float.class   | "1.337.001,87" | 1337001.9f
        float.class   | "1.337.001,87" | 1337001.9f
    }
}
