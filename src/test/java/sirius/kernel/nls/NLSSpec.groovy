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

    def "toMachineString() of Amount is properly formatted"() {
        expect:
        NLS.toMachineString(Amount.of(input)) == output
        where:
        input      | output
        123456.789 | "123456.79"
        123456.81  | "123456.81"
        0.113      | "0.11"
        -11111.1   | "-11111.10"
        1          | "1.00"
        -1         | "-1.00"
        0          | "0.00"
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

    def "toSpokenDate() formats dates and dateTimes correctly"() {
        expect:
        NLS.toSpokenDate(date) == output

        where:
        date                                 | output
        LocalDate.now()                      | "heute"
        LocalDateTime.now()                  | "vor wenigen Minuten"
        LocalDate.now().minusDays(1)         | "gestern"
        LocalDateTime.now().minusDays(1)     | "gestern"
        LocalDate.now().plusDays(1)          | "morgen"
        LocalDateTime.now().plusDays(1)      | "morgen"
        LocalDate.of(2114, 1, 1)             | "01.01.2114"
        LocalDateTime.of(2114, 1, 1, 0, 0)   | "01.01.2114"
        LocalDate.of(2014, 1, 1)             | "01.01.2014"
        LocalDateTime.of(2014, 1, 1, 0, 0)   | "01.01.2014"
        LocalDateTime.now().minusMinutes(5)  | "vor wenigen Minuten"
        LocalDateTime.now().minusMinutes(35) | "vor 35 Minuten"
        LocalDateTime.now().minusHours(1)    | "vor einer Stunde"
        LocalDateTime.now().minusHours(4)    | "vor 4 Stunden"
        LocalDateTime.now().plusMinutes(40)  | "in der nächsten Stunde"
        LocalDateTime.now().plusHours(4)     | "in 3 Stunden"

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

    def "parseUserString for a LocalTime works"() {
        expect:
        NLS.parseUserString(LocalTime.class, input) == output

        where:
        input      | output
        "14:30:12" | new LocalTime(14, 30, 12, 0)
        "14:30"    | new LocalTime(14, 30, 0, 0)
        "14"       | new LocalTime(14, 0, 0, 0)
    }

    def "getMonthNameShort correctly appends the given symbol"() {
        expect:
        NLS.getMonthNameShort(month, ".") == output

        where:
        month | output
        1     | "Jan."
        5     | "Mai"
        3     | "März"
        6     | "Juni"
        11    | "Nov."
        12    | "Dez."
        0     | ""
        13    | ""
    }
}
