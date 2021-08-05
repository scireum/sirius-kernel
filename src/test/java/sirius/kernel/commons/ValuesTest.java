/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ValuesTest {
    @Test
    void at() {
        assertEquals("A", Values.of(new String[]{"A", "B", "C"}).at(0).asString());
        assertFalse(Values.of(new String[]{"A", "B", "C"}).at(10).isFilled());
    }

    @Test
    void excelStyleColumns() {
        assertEquals("A", Values.of(new String[]{"A", "B", "C"}).at("A").asString());
        assertEquals("C", Values.of(new String[]{"A", "B", "C"}).at("C").asString());
        List<String> test = new ArrayList<>();
        for (int i = 1; i < 100; i++) {
            test.add(String.valueOf(i));
        }
        assertEquals("28", Values.of(test).at("AB").asString());
        assertEquals("34", Values.of(test).at("AH").asString());
    }
}
