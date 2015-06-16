/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

import sirius.kernel.health.Exceptions;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides a simple barrier to wait for the completion of a set of tasks represented by {@link Barrier}.
 * <p>
 * A <tt>Barrier</tt> can be used to block and wait for the completion of a given set of promises. A barrier should
 * only be used once and after a call to <tt>await</tt>, no further promises should be added. Also <tt>await</tt>
 * must only be called once.
 * <p>
 * The general call pattern looks like that:
 * <pre>
 * {@code
 *      Barrier b = Barrier.create();
 *      b.add(somePromise);
 *      b.add(anotherPromise);
 *
 *      b.await(1, TimeUnit.MINUTE);
 * }
 * </pre>
 * <p>
 * Always prefer {@link #await(long, java.util.concurrent.TimeUnit)} and specify a sane timeout since something
 * might always go wrong and a promise might therefore not complete (in time - or not at all) and your program
 * is locked forever.
 * <p>
 * This barrier can also be used in a non-blocking way, by calling {@link #asFuture()} after the last call to
 * {@link #add(Promise)}. If possible, the non-block approach should always be preferred.
 */
@ParametersAreNonnullByDefault
public class Barrier {

    private AtomicInteger promisesMade = new AtomicInteger(0);
    private AtomicInteger promisesOpen = new AtomicInteger(0);
    private Semaphore semaphore = new Semaphore(0);
    private Future completionFuture = Tasks.future();

    /**
     * Creates a new barrier.
     *
     * @return a new empty barrier.
     */
    public static Barrier create() {
        return new Barrier();
    }

    /**
     * Adds a promise to the barrier which will be waited for.
     * <p>
     * Note that one must not call <tt>add</tt> after calling <tt>await</tt>.
     *
     * @param promise the promise to wait for
     */
    @SuppressWarnings("unchecked")
    public void add(Promise<?> promise) {
        // Reset internal future...
        if (completionFuture.isCompleted()) {
            completionFuture = Tasks.future();
        }
        promisesMade.incrementAndGet();
        promisesOpen.incrementAndGet();

        ((Promise<Object>) promise).onComplete(new CompletionHandler<Object>() {
            @Override
            public void onSuccess(@Nullable Object value) throws Exception {
                semaphore.release();
                if (!completionFuture.isCompleted()) {
                    if (promisesOpen.decrementAndGet() == 0) {
                        completionFuture.success();
                    }
                }
            }

            @Override
            public void onFailure(Throwable throwable) throws Exception {
                semaphore.release();
                if (!completionFuture.isCompleted()) {
                    completionFuture.fail(throwable);
                }
            }
        });
    }

    /**
     * Generates a new {@link Future} which completes if the las added promise completes or if any one of those fails.
     *
     * @return a new future which can used to add completion handlers for all added promises.
     */
    public Future asFuture() {
        return completionFuture;
    }

    /**
     * Waits until all previously added promises completed (either successfully or failed).
     * <p>
     * Note that this method might block for an indefinite amount of time. Consider using
     * {@link #await(long, java.util.concurrent.TimeUnit)} and specify a sane timeout
     *
     * @throws InterruptedException if the thread was interrupted while waiting for completion.
     */
    public void await() throws InterruptedException {
        semaphore.acquire(promisesMade.get());
    }

    /**
     * Waits until all previously added promises completed or the given timeout expires.
     *
     * @param time the number of time intervals to wait for completion
     * @param unit the unit of time intervals to wait for completion
     * @return <tt>true</tt> if all promises completed within the given timout, <tt>false</tt> otherwise.
     */
    public boolean await(long time, TimeUnit unit) {
        try {
            return semaphore.tryAcquire(promisesMade.get(), time, unit);
        } catch (InterruptedException e) {
            Exceptions.ignore(e);
            return false;
        }
    }
}
