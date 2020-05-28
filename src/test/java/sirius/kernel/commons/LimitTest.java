/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class LimitTest {

    private void compare(String message, List<Integer> given, int... expected) {
        int[] givenArray = new int[given.size()];
        for (int i = 0; i < given.size(); i++) {
            givenArray[i] = given.get(i);
        }
        assertArrayEquals(message, expected, givenArray);
    }

    private void executeLimit(Limit limit, int expectedIterations, int... expected) {
        List<Integer> result = new ArrayList<>();
        int iterations = 0;
        for (int i = 1; i <= 10; i++) {
            iterations++;
            if (limit.nextRow()) {
                result.add(i);
            }
            if (!limit.shouldContinue()) {
                break;
            }
        }
        compare("Expected result not met for: " + limit, result, expected);
        assertEquals("Expected iterations not met for: " + limit, expectedIterations, iterations);
    }

    @Test
    public void testSingleItem() {
        executeLimit(Limit.singleItem(), 1, 1);
    }

    @Test
    public void testUnlimited() {
        executeLimit(Limit.UNLIMITED, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void testSkip() {
        executeLimit(new Limit(5, 0), 10, 6, 7, 8, 9, 10);
    }

    @Test
    public void testLimit() {
        executeLimit(new Limit(0, 5), 5, 1, 2, 3, 4, 5);
    }

    @Test
    public void testSkipAndLimit() {
        executeLimit(new Limit(2, 5), 7, 3, 4, 5, 6, 7);
    }

    @Test
    public void testBadValues() {
        // Negative start and negative limit are ignored
        executeLimit(new Limit(-1, -5), 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void testNullLimit() {
        // Null as limit is unlimited
        executeLimit(new Limit(0, null), 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void testPredicate() {
        // Use as predicate to filter a sublist of the given stream to compute a test sum
        int sum = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).filter(new Limit(2, 4).asPredicate()).mapToInt(i -> i).sum();
        assertEquals(3 + 4 + 5 + 6, sum);
    }

    @Test
    public void testRemaingItems() {
        Limit l = new Limit(5, 500);
        assertEquals(Integer.valueOf(505), l.getRemainingItems());
        l = new Limit(10, 0);
        assertNull(l.getRemainingItems());
        l = new Limit(10, 10);
        for (int i = 0; i < 12; i++) {
            l.nextRow();
        }
        assertEquals(Integer.valueOf(8), l.getRemainingItems());
        l = new Limit(10, 10);
        for (int i = 0; i < 21; i++) {
            l.nextRow();
        }
        assertEquals(Integer.valueOf(0), l.getRemainingItems());
    }
}
