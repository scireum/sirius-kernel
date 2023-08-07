package sirius.kernel.commons

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals

class RomanNumeralTest {

    @ParameterizedTest
    @CsvSource(
            "0,''",
            "1,I",
            "2,II",
            "3,III",
            "4,IV",
            "5,V",
            "6,VI",
            "7,VII",
            "8,VIII",
            "9,IX",
            "10,X",
            "20,XX",
            "40,XL",
            "50,L",
            "90,XC",
            "100,C",
            "200,CC",
            "400,CD",
            "500,D",
            "800,DCCC",
            "900,CM",
            "1000,M",
            "2000,MM"
    )
    fun `RomanNumeral converts int correctly to roman numeral strings`(input: Int, output: String) {
        assertEquals(output, RomanNumeral.toRoman(input))
    }
}
