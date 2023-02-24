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

import java.time.*

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
        CallContext.getCurrent().setLanguage("de")
        when:
        def result = NLS.toUserString(date)
        then:
        result == "09.08.2014 12:00:59"
    }

    def "toUserString() formats a LocalTime as simple time"() {
        given:
        def date = LocalTime.of(17, 23, 15)
        and:
        CallContext.getCurrent().setLanguage("de")
        when:
        def result = NLS.toUserString(date)
        then:
        result == "17:23:15"
    }

    def "toUserString() formats a LocalDate as date without time"() {
        given:
        def date = LocalDate.of(2014, 8, 9)
        and:
        CallContext.getCurrent().setLanguage("de")
        when:
        def result = NLS.toUserString(date)
        then:
        result == "09.08.2014"
    }

    def "toUserString() formats an Instant successfully"() {
        given:
        def instant = Instant.now()
        def date = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
        and:
        CallContext.getCurrent().setLanguage("de")
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
        LocalDateTime.now().plusHours(4)     | "in 3 Stunden" // this correctly rounds down to 3

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

    def "parseUserString works for integers"() {
        expect:
        NLS.parseUserString(Integer.class, input) == output

        where:
        input     | output
        "42"      | 42
        "77,0000" | 77
    }

    def "parseUserString works for longs"() {
        expect:
        NLS.parseUserString(Long.class, input) == output

        where:
        input     | output
        "12"      | 12L
        "31,0000" | 31L
    }

    def "parseUserString works for integers considering locale"() {
        expect:
        NLS.parseUserString(Integer.class, input, language) == output

        where:
        input       | language | output
        "55.000,00" | "de"     | 55000
        "56,000.00" | "en"     | 56000
    }

    def "parseUserString fails when expected"() {
        when:
        NLS.parseUserString(clazz, input) == output

        then:
        def error = thrown(expecteException)
        error.message == expectedMessage

        where:
        input        | clazz | expecteException         | expectedMessage
        "42,1"       | Integer
                .class       | IllegalArgumentException | "Bitte geben Sie eine gültige Zahl ein. '42,1' ist ungültig."
        "2999999999" | Integer
                .class       | IllegalArgumentException | "Bitte geben Sie eine gültige Zahl ein. '2999999999' ist ungültig."
        "blub"       | Double
                .class       | IllegalArgumentException | "Bitte geben Sie eine gültige Dezimalzahl ein. 'blub' ist ungültig."
    }

    def "parseUserString for a LocalTime works"() {
        expect:
        NLS.parseUserString(LocalTime.class, input) == output

        where:
        input      | output
        "14:30:12" | LocalTime.of(14, 30, 12, 0)
        "14:30"    | LocalTime.of(14, 30, 0, 0)
        "14"       | LocalTime.of(14, 0, 0, 0)
    }

    def "parseMachineString works for decimals"() {
        expect:
        NLS.parseMachineString(BigDecimal.class, input) == output

        where:
        input | output
        "0.1" | BigDecimal.ONE.divide(BigDecimal.TEN)
        "0.1" | new BigDecimal("0.1")
    }

    def "parseMachineString works for integers"() {
        expect:
        NLS.parseMachineString(Integer.class, input) == output

        where:
        input     | output
        "23"      | 23
        "90.0000" | 90
    }

    def "parseMachineString works for longs"() {
        expect:
        NLS.parseMachineString(Long.class, input) == output

        where:
        input     | output
        "5"       | 5L
        "43.0000" | 43L
    }

    def "parseMachineString fails when expected"() {
        when:
        NLS.parseMachineString(clazz, input) == output

        then:
        def error = thrown(expecteException)
        error.message == expectedMessage

        where:
        input        | clazz | expecteException         | expectedMessage
        "42.1"       | Integer
                .class       | IllegalArgumentException | "Bitte geben Sie eine gültige Zahl ein. '42.1' ist ungültig."
        "2999999999" | Integer
                .class       | IllegalArgumentException | "Bitte geben Sie eine gültige Zahl ein. '2999999999' ist ungültig."
        "blub"       | Double
                .class       | IllegalArgumentException | "Bitte geben Sie eine gültige Dezimalzahl ein. 'blub' ist ungültig."
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

    def "get with numeric works correctly"() {
        expect:
        NLS.get(property, numeric, "en") == output

        where:
        property             | numeric | output
        "nls.test.withThree" | 0       | "zero"
        "nls.test.withThree" | 1       | "one"
        "nls.test.withThree" | 2       | "many: 2"
        "nls.test.withThree" | -2      | "many: -2"
        "nls.test.withTwo"   | 0       | "many: 0"
        "nls.test.withTwo"   | 1       | "one"
        "nls.test.withTwo"   | 2       | "many: 2"
        "nls.test.withTwo"   | -2      | "many: -2"
    }

    def "unicode characters get imported without problems"() {
        given:
        def loadedProperty = NLS.get("nls.test.utf8", "en")
        expect:
        loadedProperty == ("ĀāĂăĄąĆćĈĉĊċČčĎďĐđĒēĔĕĖėĘęĚěĜĝĞğĠġĢģĤĥĦħĨĩĪīĬĭĮįİıĲĳĴĵĶķĸĹĺĻļĽľĿŀŁłŃńŅņŇňŉŊŋŌōŎŏŐőŒœŔŕŖŗŘřŚśŜŝŞşŠšŢţŤťŦŧŨũŪūŬŭŮůŰűŲųŴŵŶŷŸŹźŻżŽžſƀƁƂƃƄƅƆƇƈƉƊƋƌƍƎƏƐƑƒƓƔƕƖƗƘƙƚƛƜƝƞƟƠơƢƣƤƥƦƧƨƩƪƫƬƭƮƯưƱƲƳƴƵƶƷƸƹƺƻƼƽƾƿǀǁǂǃǄǅǆǇǈǉǊǋǌǍǎǏǐǑǒǓǔǕǖǗǘǙǚǛǜǝǞǟǠǡǢǣǤǥǦǧǨǩǪǫǬǭǮǯǰǱǲǳǴǵǺǻǼǽǾǿȀȁȂȃ...ЁЂЃЄЅІЇЈЉЊЋЌЎЏАБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдежзийклмнопрстуфхцчшщъыьэюяёђѓєѕіїјљњћќўџѠѡѢѣѤѥѦѧѨѩѪѫѬѭѮѯѰѱѲѳѴѵѶѷѸѹѺѻѼѽѾѿҀҁ҂҃...ʹ͵ͺ;΄΅Ά·ΈΉΊΌΎΏΐΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩΪΫάέήίΰαβγδεζηθικλμνξοπρςστυφχψωϊϋόύώϐϑϒϓϔϕϖϚϜϞϠϢϣϤϥϦϧϨϩϪϫϬϭϮϯϰϱϲϳ،؛؟ءآأؤإئابةتثجحخدذرزسشصضطظعغـفقكلمنهوىيًٌٍَُِّْ٠١٢٣٤٥٦٧٨٩٪٫٬٭ٰٱٲٳٴٵٶٷٸٹٺٻټٽپٿڀځڂڃڄڅچڇڈډڊڋڌڍڎڏڐڑڒړڔڕږڗژڙښڛڜڝڞڟڠڡڢڣڤڥڦڧڨکڪګڬڭڮگڰڱ...¡¢£¤¥¦§¨©ª«¬­®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ☀☁☂☃☄★☆☇☈☉☊☋☌☍☎☏☐☑☒☓☚☛☜☝☞☟☠☡☢☣☤☥☦☧☨☩☪☫☬☭☮☯☰☱☲☳☴☵☶☷☸☹☺☻☼☽☾☿♀♁♂♃♄♅♆♇♈♉♊♋♌♍♎♏♐♑♒♓♔♕♖♗♘♙♚♛♜♝♞♟♠♡♢♣♤♥♦♧♨♩♪♫♬♭♮♯✁✂✃✄✆✇✈✉✌✍✎✏✐✑✒✓✔✕✖✗✘✙✚✛✜✝✞✟✠✡✢✣✤✥✦✧✩✪✫✬✭✮✯✰✱✲✳✴✵✶✷✸✹✺✻✼✽✾✿❀❁❂❃❄❅❆❇❈❉❊❋❍❏❐❑❒❖❘❙❚❛❜❝❞❡❢❣❤❥❦❧❶❷❸❹❺❻❼❽❾❿➀➁➂➃➄➅➆➇➈➉➊➋➌➍➎➏➐➑➒➓➔➘➙➚➛➜➝...")
    }

    def "test smartGet works correctly"() {
        expect:
        NLS.smartGet(input, lang) == output
        where:
        input                 | output               | lang
        "nls.test.translate"  | "nls.test.translate" | "de"
        '$nls.test.translate' | "übersetzungs test"  | null
        '$nls.test.translate' | "übersetzungs test"  | "de"
        '$nls.test.translate' | "translation test"   | "en"
    }

    def "test various formatters"() {
        given:
        LocalDateTime date = LocalDateTime.of(2000, 1, 2, 3, 4, 5)
        expect:
        NLS.getTimeFormat("de").format(date) == "03:04"
        NLS.getTimeFormat("en").format(date) == "03:04 AM"
        NLS.getDateTimeFormat("de").format(date) == "02.01.2000 03:04:05"
        NLS.getDateTimeFormat("en").format(date) == "01/02/2000 03:04:05"
        NLS.getDateTimeFormatWithoutSeconds("de").format(date) == "02.01.2000 03:04"
        NLS.getDateTimeFormatWithoutSeconds("en").format(date) == "01/02/2000 03:04"
        NLS.getTimeFormatWithSeconds("de").format(date) == "03:04:05"
        NLS.getTimeFormatWithSeconds("en").format(date) == "03:04:05 AM"

    }

    def "formatters for null language don't throw exceptions and format using the current language"() {
        given:
        LocalDateTime date = LocalDateTime.of(2000, 1, 2, 3, 4, 5)
        when:
        String currentLang = NLS.getCurrentLanguage()
        then:
        NLS.getDateFormat(null).format(date) == NLS.getDateFormat(currentLang).format(date)
        NLS.getShortDateFormat(null).format(date) == NLS.getShortDateFormat(currentLang).format(date)
        NLS.getTimeFormatWithSeconds(null).format(date) == NLS.getTimeFormatWithSeconds(currentLang).format(date)
        NLS.getTimeFormat(null).format(date) == NLS.getTimeFormat(currentLang).format(date)
        NLS.getTimeParseFormat(null).format(date) == NLS.getTimeParseFormat(currentLang).format(date)
        NLS.getDateTimeFormat(null).format(date) == NLS.getDateTimeFormat(currentLang).format(date)
        NLS.getDateTimeFormatWithoutSeconds(null).format(date) == NLS
                .getDateTimeFormatWithoutSeconds(currentLang).format(date)
        and:
        noExceptionThrown()
    }

    def "convertDurationToDigitalClockFormat() of Duration is properly formatted"() {
        expect:
        NLS.convertDurationToDigitalClockFormat(input) == output
        where:
        input                                               | output
        Duration.ofMinutes(60L)                             | "01:00:00"
        Duration.ofMinutes(60L).plusSeconds(1L)             | "01:00:01"
        Duration.ofHours(1).plusMinutes(1L)                 | "01:01:00"
        Duration.ofHours(1).plusMinutes(1L).plusSeconds(1L) | "01:01:01"
        Duration.ofDays(2L)                                 | "48:00:00"
        Duration.ofDays(2L).plusHours(1L)                   | "49:00:00"
        Duration.ofDays(2L).plusMinutes(1L)                 | "48:01:00"
        Duration.ofSeconds(1L)                              | "00:00:01"
        Duration.ofMinutes(1L)                              | "00:01:00"
        Duration.ofMinutes(1L).plusSeconds(1L)              | "00:01:01"
    }

    def "convertDuration() of Duration is properly formatted"() {
        expect:
        NLS.convertDuration(duration, enableSeconds, enableMillis) == output
        where:
        duration                  | enableSeconds | enableMillis |
                output
        Duration
                .ofMinutes(60L)   | true          | true         | "1 Stunde"
        Duration
                .ofMinutes(60L)   | true          | false        | "1 Stunde"
        Duration
                .ofMinutes(60L)   | false         | true         | "1 Stunde"
        Duration
                .ofMinutes(60L)   | false         | false        | "1 Stunde"
        Duration
                .ofHours(2L)      | true          | true         | "2 Stunden"
        Duration
                .ofHours(2L)      | true          | false        | "2 Stunden"
        Duration
                .ofHours(2L)      | false         | true         | "2 Stunden"
        Duration
                .ofHours(2L)      | false         | false        | "2 Stunden"
        Duration
                .ofMinutes(61L)   | true          | true         | "1 Stunde, 1 Minute"
        Duration
                .ofMinutes(61L)   | true          | false        | "1 Stunde, 1 Minute"
        Duration
                .ofMinutes(61L)   | false         | true         | "1 Stunde, 1 Minute"
        Duration
                .ofMinutes(61L)   | false         | false        | "1 Stunde, 1 Minute"
        Duration
                .ofSeconds(61L)   | true          | true         | "1 Minute, 1 Sekunde"
        Duration
                .ofSeconds(61L)   | true          | false        | "1 Minute, 1 Sekunde"
        Duration
                .ofSeconds(61L)   | false         | true         | "1 Minute"
        Duration
                .ofSeconds(61L)   | false         | false        | "1 Minute"
        Duration
                .ofSeconds(121L)  | false         | false        | "2 Minuten"
        Duration
                .ofSeconds(122L)  | true          | false        | "2 Minuten, 2 Sekunden"
        Duration
                .ofDays(122L)     | false         | false        | "122 Tage"
        Duration
                .ofDays(1L)       | false         | false        | "1 Tag"
        Duration
                .ofDays(1L)
                .plusMinutes(30L) | true          | true         | "1 Tag, 30 Minuten"
        Duration
                .ofDays(1L)
                .plusHours(7)
                .plusMinutes(30L) | true          | true         | "1 Tag, 7 Stunden, 30 Minuten"
        Duration
                .ofDays(1L)
                .plusHours(24)
                .plusMinutes(30L) | true          | true         | "2 Tage, 30 Minuten"
        Duration
                .ofDays(2L)
                .plusHours(2)
                .plusMinutes(30L) | true          | true         | "2 Tage, 2 Stunden, 30 Minuten"
        Duration
                .ofDays(2L)
                .plusHours(2)
                .plusMinutes(30L)
                .plusSeconds(22L) | true          | true         | "2 Tage, 2 Stunden, 30 Minuten, 22 Sekunden"
        Duration
                .ofDays(2L)
                .plusHours(2)
                .plusMinutes(30L)
                .plusSeconds(22L) | false         | true         | "2 Tage, 2 Stunden, 30 Minuten"
        Duration
                .ofDays(2L)
                .plusHours(2)
                .plusMinutes(30L)
                .plusSeconds(22L)
                .plusMillis(1L)   | false         | true         | "2 Tage, 2 Stunden, 30 Minuten"
        Duration
                .ofDays(2L)
                .plusHours(2)
                .plusMinutes(30L)
                .plusSeconds(22L)
                .plusMillis(1L)   | true          | true         | "2 Tage, 2 Stunden, 30 Minuten, 22 Sekunden, 1 Millisekunde"
        Duration
                .ofDays(2L)
                .plusHours(2)
                .plusMinutes(30L)
                .plusMillis(1L)   | true          | true         | "2 Tage, 2 Stunden, 30 Minuten, 1 Millisekunde"
        Duration
                .ofDays(2L)
                .plusHours(2)
                .plusMinutes(30L)
                .plusMillis(33L)  | true          | true         | "2 Tage, 2 Stunden, 30 Minuten, 33 Millisekunden"
        Duration
                .ofDays(0L)       | true          | true         | ""
        null                      | true          | true         | ""
        Duration.ofMillis(101L)   | false         | false        | ""
        Duration.ofMillis(101L)   | true          | false        | ""
        Duration.ofMillis(101L)   | true          | true         | "101 Millisekunden"
        Duration.ofSeconds(33L)   | true          | true         | "33 Sekunden"
        Duration.ofSeconds(33L)   | true          | false        | "33 Sekunden"
        Duration.ofSeconds(33L)   | false         | false        | ""
    }
}
