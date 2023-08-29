/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.nls

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import sirius.kernel.SiriusExtension
import sirius.kernel.async.CallContext
import sirius.kernel.commons.Amount
import java.math.BigDecimal
import java.time.*
import java.util.stream.Stream
import kotlin.test.assertEquals


/**
 * Tests the [NLS] class.
 */
@ExtendWith(SiriusExtension::class)

class NLSTest {

    companion object {
        @JvmStatic
        private fun `generator for parseUserString fails when expected`(): Stream<Arguments?>? {
            return Stream.of(
                    Arguments.of(
                            "42.1",
                            Integer::class.java,
                            IllegalArgumentException::class.java,
                            "Bitte geben Sie eine gültige Zahl ein. '42.1' ist ungültig."
                    ),
                    Arguments.of(
                            "42,1",
                            Integer::class.java,
                            IllegalArgumentException::class.java,
                            "Bitte geben Sie eine gültige Zahl ein. '42,1' ist ungültig."
                    ),
                    Arguments.of(
                            "2999999999",
                            Integer::class.java, IllegalArgumentException::class.java,
                            "Bitte geben Sie eine gültige Zahl ein. '2999999999' ist ungültig."
                    ),
                    Arguments.of(
                            "blub",
                            Double::class.java, IllegalArgumentException::class.java,
                            "Bitte geben Sie eine gültige Dezimalzahl ein. 'blub' ist ungültig."
                    )
            )
        }

        @JvmStatic
        private fun `generator for parseUserString for a LocalTime works`(): Stream<Arguments?>? {
            return Stream.of(
                    Arguments.of(
                            "14:30:12", LocalTime.of(14, 30, 12, 0)
                    ),
                    Arguments.of(
                            "14:30", LocalTime.of(14, 30, 0, 0)
                    ),
                    Arguments.of(
                            "14", LocalTime.of(14, 0, 0, 0)
                    )
            )
        }
    }

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

    @Test
    fun `parseUserString with LocalTime parses 900 and 90023`() {
        val input = "9:00"
        val inputWithSeconds = "9:00:23"
        assertEquals(9, NLS.parseUserString(LocalTime::class.java, input).getHour())
        assertEquals(9, NLS.parseUserString(LocalTime::class.java, inputWithSeconds).getHour())
    }

    @Test
    fun `parseUserString for an Amount works`() {
        val input = "34,54"
        assertEquals("34,54", NLS.parseUserString(Amount::class.java, input).toString())
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|', textBlock = """
        42      | 42
        77,0000 | 77"""
    )
    fun `parseUserString works for integers`(input: String, output: Int) {
        assertEquals(output, NLS.parseUserString(Int::class.java, input))
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|', textBlock = """
        12      | 12
        31,0000 | 31"""
    )
    fun `parseUserString works for longs`(input: String, output: Long) {
        assertEquals(output, NLS.parseUserString(Long::class.java, input))
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|', textBlock = """
         55.000,00 | de     | 55000
        56,000.00 | en     | 56000"""
    )
    fun `parseUserString works for integers considering locale`(input: String, language: String, output: Int) {
        assertEquals(output, NLS.parseUserString(Int::class.java, input, language))
    }

    @ParameterizedTest
    @MethodSource("generator for parseUserString fails when expected")
    fun `parseUserString fails when expected`(
            input: String,
            type: Class<out Number>,
            exception: Class<Exception>,
            message: String
    ) {
        val thrown: Exception = Assertions.assertThrows(exception) {

            NLS.parseUserString(type, input)
        }
        Assertions.assertEquals(message, thrown.message)
    }

    @ParameterizedTest
    @MethodSource("generator for parseUserString for a LocalTime works")
    fun `parseUserString for a LocalTime works`(input: String, output: LocalTime) {
        assertEquals(output, NLS.parseUserString(LocalTime::class.java, input))
    }

    @Test
    fun `parseMachineString works for decimals`() {
        assertEquals(BigDecimal.ONE.divide(BigDecimal.TEN), NLS.parseMachineString(BigDecimal::class.java, "0.1"))
        assertEquals(BigDecimal("0.1"), NLS.parseMachineString(BigDecimal::class.java, "0.1"))
    }

    @Test
    fun `parseMachineString works for integers`() {
        assertEquals(23, NLS.parseMachineString(Int::class.java, "23"))
        assertEquals(90, NLS.parseMachineString(Int::class.java, "90.0000"))
    }

    @Test
    fun `parseMachineString works for longs`() {
        assertEquals(5L, NLS.parseMachineString(Long::class.java, "5"))
        assertEquals(43L, NLS.parseMachineString(Long::class.java, "43.0000"))
    }

    @ParameterizedTest
    @MethodSource("generator for parseUserString fails when expected")
    fun `parseMachineString fails when expected`(
            input: String,
            type: Class<out Number>,
            exception: Class<Exception>,
            message: String
    ) {
        val thrown: Exception = Assertions.assertThrows(exception) {
            NLS.parseMachineString(type, input)
        }
        Assertions.assertEquals(message, thrown.message)
    }

    @Test
    fun `getMonthNameShort correctly appends the given symbol`() {
        Assertions.assertEquals("Jan.", NLS.getMonthNameShort(1, "."))
        Assertions.assertEquals("Mai", NLS.getMonthNameShort(5, "."))
        Assertions.assertEquals("März", NLS.getMonthNameShort(3, "."))
        Assertions.assertEquals("Juni", NLS.getMonthNameShort(6, "."))
        Assertions.assertEquals("Nov.", NLS.getMonthNameShort(11, "."))
        Assertions.assertEquals("Dez.", NLS.getMonthNameShort(12, "."))
        Assertions.assertEquals("", NLS.getMonthNameShort(0, "."))
        Assertions.assertEquals("", NLS.getMonthNameShort(13, "."))
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|', textBlock = """
        nls.test.withThree | 0      | zero
        nls.test.withThree | 1      | one
        nls.test.withThree | 2      | many: 2X
        nls.test.withThree | -2     | many: -2X
        nls.test.withTwo  | 0       | many: 0X
        nls.test.withTwo  | 1       | one
        nls.test.withTwo  | 2       | many: 2X
        nls.test.withTwo  | -2      | many: -2X"""
    )
    fun `get with numeric and autoformat works correctly`(property: String, numeric: Int, output: String) {
        val function: (Int) -> String = { number -> number.toString() + 'X' }
        Assertions.assertEquals(output, NLS.get(property, numeric, function, "en"))
    }

    @Test
    fun `unicode characters get imported without problems`() {
        val loadedProperty = NLS.get("nls.test.utf8", "en")
        Assertions.assertEquals(
                "ĀāĂăĄąĆćĈĉĊċČčĎďĐđĒēĔĕĖėĘęĚěĜĝĞğĠġĢģĤĥĦħĨĩĪīĬĭĮįİıĲĳĴĵĶķĸĹĺĻļĽľĿŀŁłŃńŅņŇňŉŊŋŌōŎŏŐőŒœŔŕŖŗŘřŚśŜŝŞşŠšŢţŤťŦŧŨũŪūŬŭŮůŰűŲųŴŵŶŷŸŹźŻżŽžſƀƁƂƃƄƅƆƇƈƉƊƋƌƍƎƏƐƑƒƓƔƕƖƗƘƙƚƛƜƝƞƟƠơƢƣƤƥƦƧƨƩƪƫƬƭƮƯưƱƲƳƴƵƶƷƸƹƺƻƼƽƾƿǀǁǂǃǄǅǆǇǈǉǊǋǌǍǎǏǐǑǒǓǔǕǖǗǘǙǚǛǜǝǞǟǠǡǢǣǤǥǦǧǨǩǪǫǬǭǮǯǰǱǲǳǴǵǺǻǼǽǾǿȀȁȂȃ...ЁЂЃЄЅІЇЈЉЊЋЌЎЏАБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдежзийклмнопрстуфхцчшщъыьэюяёђѓєѕіїјљњћќўџѠѡѢѣѤѥѦѧѨѩѪѫѬѭѮѯѰѱѲѳѴѵѶѷѸѹѺѻѼѽѾѿҀҁ҂҃...ʹ͵ͺ;΄΅Ά·ΈΉΊΌΎΏΐΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩΪΫάέήίΰαβγδεζηθικλμνξοπρςστυφχψωϊϋόύώϐϑϒϓϔϕϖϚϜϞϠϢϣϤϥϦϧϨϩϪϫϬϭϮϯϰϱϲϳ،؛؟ءآأؤإئابةتثجحخدذرزسشصضطظعغـفقكلمنهوىيًٌٍَُِّْ٠١٢٣٤٥٦٧٨٩٪٫٬٭ٰٱٲٳٴٵٶٷٸٹٺٻټٽپٿڀځڂڃڄڅچڇڈډڊڋڌڍڎڏڐڑڒړڔڕږڗژڙښڛڜڝڞڟڠڡڢڣڤڥڦڧڨکڪګڬڭڮگڰڱ...¡¢£¤¥¦§¨©ª«¬­®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ☀☁☂☃☄★☆☇☈☉☊☋☌☍☎☏☐☑☒☓☚☛☜☝☞☟☠☡☢☣☤☥☦☧☨☩☪☫☬☭☮☯☰☱☲☳☴☵☶☷☸☹☺☻☼☽☾☿♀♁♂♃♄♅♆♇♈♉♊♋♌♍♎♏♐♑♒♓♔♕♖♗♘♙♚♛♜♝♞♟♠♡♢♣♤♥♦♧♨♩♪♫♬♭♮♯✁✂✃✄✆✇✈✉✌✍✎✏✐✑✒✓✔✕✖✗✘✙✚✛✜✝✞✟✠✡✢✣✤✥✦✧✩✪✫✬✭✮✯✰✱✲✳✴✵✶✷✸✹✺✻✼✽✾✿❀❁❂❃❄❅❆❇❈❉❊❋❍❏❐❑❒❖❘❙❚❛❜❝❞❡❢❣❤❥❦❧❶❷❸❹❺❻❼❽❾❿➀➁➂➃➄➅➆➇➈➉➊➋➌➍➎➏➐➑➒➓➔➘➙➚➛➜➝...",
                loadedProperty
        )
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|', textBlock =
    "nls.test.translate  | nls.test.translate | de \n\$nls.test.translate | übersetzungs test  | null\n\$nls.test.translate | übersetzungs test  | de\n\$nls.test.translate | translation test   | en"
    )
    fun `test smartGet works correctly`(input: String, output: String, lang: String) {
        Assertions.assertEquals(output, NLS.smartGet(input, lang))
    }

    @Test
    fun `test various formatters`() {
        val date = LocalDateTime.of(2000, 1, 2, 3, 4, 5)
        Assertions.assertEquals("03:04", NLS.getTimeFormat("de").format(date))
        Assertions.assertEquals("03:04 AM", NLS.getTimeFormat("en").format(date))
        Assertions.assertEquals("02.01.2000 03:04:05", NLS.getDateTimeFormat("de").format(date))
        Assertions.assertEquals("01/02/2000 03:04:05", NLS.getDateTimeFormat("en").format(date))
        Assertions.assertEquals("02.01.2000 03:04", NLS.getDateTimeFormatWithoutSeconds("de").format(date))
        Assertions.assertEquals("01/02/2000 03:04", NLS.getDateTimeFormatWithoutSeconds("en").format(date))
        Assertions.assertEquals("03:04:05", NLS.getTimeFormatWithSeconds("de").format(date))
        Assertions.assertEquals("03:04:05 AM", NLS.getTimeFormatWithSeconds("en").format(date))
    }

    @Test
    fun `formatters for null language don't throw exceptions and format using the current language`() {
        val date = LocalDateTime.of(2000, 1, 2, 3, 4, 5)
        val currentLang = NLS.getCurrentLanguage()
        Assertions.assertEquals(NLS.getDateFormat(currentLang).format(date), NLS.getDateFormat(null).format(date))
        Assertions.assertDoesNotThrow { NLS.getDateFormat(null).format(date) }
        Assertions.assertEquals(
                NLS.getShortDateFormat(currentLang).format(date),
                NLS.getShortDateFormat(null).format(date)
        )
        Assertions.assertDoesNotThrow { NLS.getShortDateFormat(null).format(date) }
        Assertions.assertEquals(
                NLS.getTimeFormatWithSeconds(currentLang).format(date),
                NLS.getTimeFormatWithSeconds(null).format(date)
        )
        Assertions.assertDoesNotThrow { NLS.getTimeFormatWithSeconds(null).format(date) }
        Assertions.assertEquals(NLS.getTimeFormat(currentLang).format(date), NLS.getTimeFormat(null).format(date))
        Assertions.assertDoesNotThrow { NLS.getTimeFormat(null).format(date) }
        Assertions.assertEquals(
                NLS.getTimeParseFormat(currentLang).format(date),
                NLS.getTimeParseFormat(null).format(date)
        )
        Assertions.assertDoesNotThrow { NLS.getTimeParseFormat(null).format(date) }
        Assertions.assertEquals(
                NLS.getDateTimeFormat(currentLang).format(date),
                NLS.getDateTimeFormat(null).format(date)
        )
        Assertions.assertDoesNotThrow { NLS.getDateTimeFormat(null).format(date) }
        Assertions.assertEquals(
                NLS
                        .getDateTimeFormatWithoutSeconds(currentLang).format(date),
                NLS.getDateTimeFormatWithoutSeconds(null).format(date)
        )
        Assertions.assertDoesNotThrow { NLS.getDateTimeFormatWithoutSeconds(null).format(date) }
    }

    @Test
    fun `convertDurationToDigitalClockFormat() of Duration is properly formatted`() {
        Assertions.assertEquals("01:00:00", NLS.convertDurationToDigitalClockFormat(Duration.ofMinutes(60L)))
        Assertions.assertEquals(
                "01:00:01",
                NLS.convertDurationToDigitalClockFormat(Duration.ofMinutes(60L).plusSeconds(1L))
        )
        Assertions.assertEquals(
                "01:01:00",
                NLS.convertDurationToDigitalClockFormat(Duration.ofHours(1).plusMinutes(1L))
        )
        Assertions.assertEquals(
                "01:01:01",
                NLS.convertDurationToDigitalClockFormat(Duration.ofHours(1).plusMinutes(1L).plusSeconds(1L))
        )
        Assertions.assertEquals("48:00:00", NLS.convertDurationToDigitalClockFormat(Duration.ofDays(2L)))
        Assertions.assertEquals("49:00:00", NLS.convertDurationToDigitalClockFormat(Duration.ofDays(2L).plusHours(1L)))
        Assertions.assertEquals(
                "48:01:00",
                NLS.convertDurationToDigitalClockFormat(Duration.ofDays(2L).plusMinutes(1L))
        )
        Assertions.assertEquals("00:00:01", NLS.convertDurationToDigitalClockFormat(Duration.ofSeconds(1L)))
        Assertions.assertEquals("00:01:00", NLS.convertDurationToDigitalClockFormat(Duration.ofMinutes(1L)))
        Assertions.assertEquals(
                "00:01:01",
                NLS.convertDurationToDigitalClockFormat(Duration.ofMinutes(1L).plusSeconds(1L))
        )
    }

    @Test
    fun `convertDuration() of Duration is properly formatted`() {
        assertEquals("1 Stunde", NLS.convertDuration(Duration.ofMinutes(60L), true, true))
        assertEquals("1 Stunde", NLS.convertDuration(Duration.ofMinutes(60L), true, false))
        assertEquals("1 Stunde", NLS.convertDuration(Duration.ofMinutes(60L), false, true))
        assertEquals("1 Stunde", NLS.convertDuration(Duration.ofMinutes(60L), false, false))
        assertEquals("2 Stunden", NLS.convertDuration(Duration.ofHours(2L), true, true))
        assertEquals("2 Stunden", NLS.convertDuration(Duration.ofHours(2L), true, false))
        assertEquals("2 Stunden", NLS.convertDuration(Duration.ofHours(2L), false, true))
        assertEquals("2 Stunden", NLS.convertDuration(Duration.ofHours(2L), false, false))
        assertEquals("1 Stunde, 1 Minute", NLS.convertDuration(Duration.ofMinutes(61L), true, true))
        assertEquals("1 Stunde, 1 Minute", NLS.convertDuration(Duration.ofMinutes(61L), true, false))
        assertEquals("1 Stunde, 1 Minute", NLS.convertDuration(Duration.ofMinutes(61L), false, true))
        assertEquals("1 Stunde, 1 Minute", NLS.convertDuration(Duration.ofMinutes(61L), false, false))
        assertEquals("1 Minute, 1 Sekunde", NLS.convertDuration(Duration.ofSeconds(61L), true, true))
        assertEquals("1 Minute, 1 Sekunde", NLS.convertDuration(Duration.ofSeconds(61L), true, false))
        assertEquals("1 Minute", NLS.convertDuration(Duration.ofSeconds(61L), false, true))
        assertEquals("1 Minute", NLS.convertDuration(Duration.ofSeconds(61L), false, false))
        assertEquals("2 Minuten", NLS.convertDuration(Duration.ofSeconds(121L), false, false))
        assertEquals("2 Minuten, 2 Sekunden", NLS.convertDuration(Duration.ofSeconds(122L), true, false))
        assertEquals("122 Tage", NLS.convertDuration(Duration.ofDays(122L), false, false))
        assertEquals("1 Tag", NLS.convertDuration(Duration.ofDays(1L), false, false))
        assertEquals("1 Tag, 30 Minuten", NLS.convertDuration(Duration.ofDays(1L).plusMinutes(30L), true, true))
        assertEquals(
                "1 Tag, 7 Stunden, 30 Minuten",
                NLS.convertDuration(Duration.ofDays(1L).plusHours(7).plusMinutes(30L), true, true)
        )
        assertEquals(
                "2 Tage, 30 Minuten",
                NLS.convertDuration(Duration.ofDays(1L).plusHours(24).plusMinutes(30L), true, true)
        )
        assertEquals(
                "2 Tage, 2 Stunden, 30 Minuten",
                NLS.convertDuration(Duration.ofDays(2L).plusHours(2).plusMinutes(30L), true, true)
        )
        assertEquals(
                "2 Tage, 2 Stunden, 30 Minuten, 22 Sekunden",
                NLS.convertDuration(Duration.ofDays(2L).plusHours(2).plusMinutes(30L).plusSeconds(22L), true, true)
        )
        assertEquals(
                "2 Tage, 2 Stunden, 30 Minuten",
                NLS.convertDuration(Duration.ofDays(2L).plusHours(2).plusMinutes(30L).plusSeconds(22L), false, true)
        )
        assertEquals(
                "2 Tage, 2 Stunden, 30 Minuten",
                NLS.convertDuration(
                        Duration.ofDays(2L).plusHours(2).plusMinutes(30L).plusSeconds(22L).plusMillis(1L),
                        false,
                        true
                )
        )
        assertEquals(
                "2 Tage, 2 Stunden, 30 Minuten, 22 Sekunden, 1 Millisekunde",
                NLS.convertDuration(
                        Duration.ofDays(2L).plusHours(2).plusMinutes(30L).plusSeconds(22L).plusMillis(1L),
                        true,
                        true
                )
        )
        assertEquals(
                "2 Tage, 2 Stunden, 30 Minuten, 1 Millisekunde",
                NLS.convertDuration(Duration.ofDays(2L).plusHours(2).plusMinutes(30L).plusMillis(1L), true, true)
        )
        assertEquals(
                "2 Tage, 2 Stunden, 30 Minuten, 33 Millisekunden",
                NLS.convertDuration(Duration.ofDays(2L).plusHours(2).plusMinutes(30L).plusMillis(33L), true, true)
        )
        assertEquals("", NLS.convertDuration(Duration.ofDays(0L), true, true))
        assertEquals("", NLS.convertDuration(null, true, true))
        assertEquals("", NLS.convertDuration(Duration.ofMillis(101L), false, false))
        assertEquals("", NLS.convertDuration(Duration.ofMillis(101L), true, false))
        assertEquals("101 Millisekunden", NLS.convertDuration(Duration.ofMillis(101L), true, true))
        assertEquals("33 Sekunden", NLS.convertDuration(Duration.ofSeconds(33L), true, true))
        assertEquals("33 Sekunden", NLS.convertDuration(Duration.ofSeconds(33L), true, false))
        assertEquals("", NLS.convertDuration(Duration.ofSeconds(33L), false, false))
    }

}
