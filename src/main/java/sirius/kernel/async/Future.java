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
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public class Future extends Promise<Object> {

    /**
     * Marks this future as successfully completed.
     * <p>
     * This is a shortcut for <code>success(null)</code>, which further shows, that the matter of successful
     * completion is important, not the resulting object of a computation.
     */
    public void success() {
        success(null);
    }

}
