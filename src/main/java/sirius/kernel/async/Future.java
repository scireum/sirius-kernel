/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

/**
 * Represents an untyped {@link Promise} where the completion or failure is more important than the result itself.
 * <p>
 * Provides all possibilities or error chaining, aggregating or non-blocking listening like a common promise but
 * without a concrete result object in mind.
 */
public class Future extends Promise<Object> {

    /**
     * Marks this future as successfully completed.
     * <p>
     * This is a shortcut for {@code success(null)}, which further shows, that the matter of successful
     * completion is important, not the resulting object of a computation.
     *
     * @return <tt>this</tt> for fluent method chaining
     */
    public Future success() {
        success(null);

        return this;
    }

    /**
     * Adds a completion handler to this promise which only handles the successful completion of the promise.
     * <p>
     * If the promise is already completed, the handler is immediately invoked.
     *
     * @param successHandler the handler to be notified once the promise is completed. A promise can notify more than
     *                       one handler.
     * @return <tt>this</tt> for fluent method chaining
     */
    public Future onSuccess(Runnable successHandler) {
        onSuccess(ignored -> successHandler.run());

        return this;
    }
}
