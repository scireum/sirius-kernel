/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Provides tests for {@link CombinedFuture}
 */
class CombinedFutureTest {

    @Test
    void emptyCombinedFutureDontBlock() {
        CombinedFuture combinedFuture = new CombinedFuture();
        assertTrue(combinedFuture.asFuture().isCompleted());
    }

    @Test
    void combinedFutureActuallyAwait() {
        CombinedFuture combinedFuture = new CombinedFuture();
        Future future = new Future();
        combinedFuture.add(future);
        assertFalse(combinedFuture.asFuture().isCompleted());
        future.success();
        assertTrue(combinedFuture.asFuture().isCompleted());
    }

    @Test
    void combinedFutureWorkWithCompletedFutures() {
        CombinedFuture combinedFuture = new CombinedFuture();
        Future future = new Future().success();
        combinedFuture.add(future);
        assertTrue(combinedFuture.asFuture().isCompleted());
    }

    @Test
    void combinedFutureWorkWithFailingFutures() {
        CombinedFuture combinedFuture = new CombinedFuture();
        Future future = new Future();
        combinedFuture.add(future);
        future.fail(new IllegalStateException("ThisIsExpected"));
        assertTrue(combinedFuture.asFuture().isCompleted());
    }
}
