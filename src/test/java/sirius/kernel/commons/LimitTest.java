/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class LimitTest {

    private void compare(String message, List<Integer> given, int... expected) {
        int[] givenArray = new int[given.size()];
        for (int i = 0; i < given.size(); i++) {
            givenArray[i] = given.get(i);
        }
        assertArrayEquals(message, expected, givenArray);
    }

    private void executeLimit(Limit limit, int expectedIterations, int... expected) {
        List<Integer> result = Lists.newArrayList();
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
        int sum = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                        .filter(new Limit(2, 4).asPredicate())
                        .collect(Collectors.summingInt(i -> i));
        assertEquals(3 + 4 + 5 + 6, sum);
    }

}
