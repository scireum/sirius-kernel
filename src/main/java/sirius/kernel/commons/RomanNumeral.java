/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

/**
 * Converts integers to roman numerals.
 */
public class RomanNumeral {
    private static final String[] RCODE = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
    private static final int[] BVAL = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};

    private RomanNumeral() {
        // Utility class without public constructor
    }

    /**
     * Converts the given value to a roman representation
     *
     * @param value the value to convert. This must be &gt;0 and &lt;4000.
     * @return a representation of the given number as roman numerals
     */
    public static String toRoman(int value) {
        if (value <= 0 || value >= 4000) {
            return "";
        }
        StringBuilder roman = new StringBuilder();
        int currentValue = value;
        for (int i = 0; i < RCODE.length; i++) {
            while (currentValue >= BVAL[i]) {
                currentValue -= BVAL[i];
                roman.append(RCODE[i]);
            }
        }
        return roman.toString();
    }
}
