/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Handler which can be attached to instances of {@link Promise} to be notified once a value is available or when the
 * computation failed.
 *
 * @param <V> the type of the expected value to be computed by the Promise
 */
public interface CompletionHandler<V> {
    /**
     * Invoked if the promise is successfully completed with the given value.
     *
     * @param value the computed value of the promise.
     * @throws Exception simplifies exception handling as each error is either passed on to the next promise or logged
     *                   via {@link sirius.kernel.health.Exceptions#handle()}
     */
    void onSuccess(@Nullable V value) throws Exception;

    /**
     * Invoked if the promise is fails with the given throwable.
     *
     * @param throwable the thrown error which occurred while computing the promised value.
     * @throws Exception simplifies exception handling as each error is either passed on to the next promise or logged
     *                   via {@link sirius.kernel.health.Exceptions#handle()}
     */
    void onFailure(@Nonnull Throwable throwable) throws Exception;
}
