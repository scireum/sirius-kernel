/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Permits to await the completion of multiple {@link Promise promises} or {@link Future futures}.
 * <p>
 * A CombinedFuture should only be used once and after a call to <tt>asFuture</tt>, no further promises should be added.
 * <p>
 * The general call pattern looks like that:
 * <pre>
 * {@code
 *      CombinedFuture b = new CombinedFuture();
 *      b.add(somePromise);
 *      b.add(anotherPromise);
 *
 *      b.asFuture().await(Duration.ofMinutes(1));
 * }
 * </pre>
 */
@ParametersAreNonnullByDefault
public class CombinedFuture {

    private final AtomicInteger promisesOpen = new AtomicInteger(0);
    private Throwable lastError;
    private Future completionFuture;

    /**
     * Adds a promise to waited for.
     * <p>
     * Note that one must not call <tt>add</tt> after calling <tt>await</tt>.
     *
     * @param promise the promise to wait for
     */
    @SuppressWarnings("unchecked")
    public void add(Promise<?> promise) {
        if (completionFuture != null) {
            throw new IllegalStateException(
                    "Cannot add more promises to a CombinedFuture after await or asFuture has been called!");
        }

        promisesOpen.incrementAndGet();

        ((Promise<Object>) promise).onComplete(new CompletionHandler<Object>() {
            @Override
            public void onSuccess(@Nullable Object value) throws Exception {
                handleSuccessfulPromise();
            }

            @Override
            public void onFailure(Throwable throwable) throws Exception {
                handleFailedPromise(throwable);
            }
        });
    }

    private void handleSuccessfulPromise() {
        if (promisesOpen.decrementAndGet() > 0) {
            return;
        }
        if (completionFuture == null) {
            return;
        }
        if (lastError != null) {
            completionFuture.fail(lastError);
        } else {

            completionFuture.success();
        }
    }

    private void handleFailedPromise(Throwable throwable) {
        if (promisesOpen.decrementAndGet() > 0) {
            return;
        }

        if (completionFuture != null) {
            completionFuture.fail(throwable);
        } else {
            lastError = throwable;
        }
    }

    /**
     * Generates a new {@link Future} which completes if the last added promise completes or if any one of those fails.
     *
     * @return a new future which can used to add completion handlers for all added promises.
     */
    public Future asFuture() {
        if (completionFuture == null) {
            completionFuture = new Future();
            if (promisesOpen.get() == 0) {
                if (lastError != null) {
                    completionFuture.fail(lastError);
                } else {

                    completionFuture.success();
                }
            }
        }

        return completionFuture;
    }
}
