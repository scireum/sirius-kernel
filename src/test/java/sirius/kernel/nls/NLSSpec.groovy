/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.nls

import sirius.kernel.async.CallContext
import sirius.kernel.BaseSpecification

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class NLSSpec extends BaseSpecification {

    def "toMachineString() formats a LocalDate as date without time"() {
        given:
        def date = LocalDate.of(2014, 8, 9);
        when:
        def result = NLS.toMachineString(date);
        then:
        result == "2014-08-09";
    }

    def "toMachineString() formats a LocalDateTime as date with time"() {
        given:
        def date = LocalDateTime.of(2014, 8, 9, 12, 00, 59);
        when:
        def result = NLS.toMachineString(date);
        then:
        result == "2014-08-09 12:00:59";
    }

    def "toUserString() formats a LocalDateTime as date with time"() {
        given:
        def date = LocalDateTime.of(2014, 8, 9, 12, 00, 59);
        and:
        CallContext.getCurrent().setLang("de");
        when:
        def result = NLS.toUserString(date);
        then:
        result == "09.08.2014 12:00:59";
    }

    def "toUserString() formats a LocalTime as simple time"() {
        given:
        def date = LocalTime.of(17, 23, 15);
        and:
        CallContext.getCurrent().setLang("de");
        when:
        def result = NLS.toUserString(date);
        then:
        result == "17:23:15";
    }

    def "toUserString() formats a LocalDate as date without time"() {
        given:
        def date = LocalDate.of(2014, 8, 9);
        and:
        CallContext.getCurrent().setLang("de");
        when:
        def result = NLS.toUserString(date);
        then:
        result == "09.08.2014";
    }

    def "toUserString() formats an Instant successfully"() {
        given:
        def date = LocalDateTime.now();
        def instant = Instant.now();
        and:
        CallContext.getCurrent().setLang("de");
        when:
        def dateFormatted = NLS.toUserString(date);
        def instantFormatted = NLS.toUserString(instant);
        then:
        dateFormatted == instantFormatted;
    }

    def "toUserString() formats null as empty string"() {
        given:
        def input = null;
        when:
        def result = NLS.toUserString(input);
        then:
        result == ""
    }

}
