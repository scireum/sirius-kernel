/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

import org.junit.Assert;
import org.junit.Test;

/**
 * Provides tests for {@link CombinedFuture}
 */
public class CombinedFutureTest {

    @Test
    public void emptyCombinedFutureDontBlock() {
        CombinedFuture combinedFuture = new CombinedFuture();
        Assert.assertTrue(combinedFuture.asFuture().isCompleted());
    }

    @Test
    public void combinedFutureActuallyAwait() {
        CombinedFuture combinedFuture = new CombinedFuture();
        Future future = new Future();
        combinedFuture.add(future);
        Assert.assertFalse(combinedFuture.asFuture().isCompleted());
        future.success();
        Assert.assertTrue(combinedFuture.asFuture().isCompleted());
    }

    @Test
    public void combinedFutureWorkWithCompletedFutures() {
        CombinedFuture combinedFuture = new CombinedFuture();
        Future future = new Future().success();
        combinedFuture.add(future);
        Assert.assertTrue(combinedFuture.asFuture().isCompleted());
    }

    @Test
    public void combinedFutureWorkWithFailingFutures() {
        CombinedFuture combinedFuture = new CombinedFuture();
        Future future = new Future();
        combinedFuture.add(future);
        future.fail(new IllegalStateException("ThisIsExpected"));
        Assert.assertTrue(combinedFuture.asFuture().isCompleted());
    }
}
