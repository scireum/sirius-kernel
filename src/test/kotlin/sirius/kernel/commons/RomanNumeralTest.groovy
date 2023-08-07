package sirius.kernel.commons

import sirius.kernel.BaseSpecification

class RomanNumeralTest extends BaseSpecification {

    def "RomanNumeral converts int correctly to roman numeral strings"() {
        expect:
        RomanNumeral.toRoman(input) == output
        where:
        input | output
         0    | ""
         1    | "I"
         2    | "II"
         3    | "III"
         4    | "IV"
         5    | "V"
         6    | "VI"
         7    | "VII"
         8    | "VIII"
         9    | "IX"
         10   | "X"
         20   | "XX"
         40   | "XL"
         50   | "L"
         90   | "XC"
         100  | "C"
         200  | "CC"
         400  | "CD"
         500  | "D"
         800  | "DCCC"
         900  | "CM"
         1000 | "M"
         2000 | "MM"
    }
}
