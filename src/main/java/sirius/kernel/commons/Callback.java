/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

/**
 * Implements the {@link java.util.function.Consumer} pattern which permits the handler to throw an exception.
 * <p>
 * This sometimes might simplify exception handling. If this feature is not required use a plain
 * {@link java.util.function.Consumer} instead.
 * <p>
 * If the lambda is only permitted to throw a specific exception and should handle everything
 * else internally, use a {@link ThrowingCallback}.
 *
 * @param <T> the type of the object passed to the callback
 * @see Processor
 * @see Producer
 * @see ThrowingCallback
 */
@FunctionalInterface
public interface Callback<T> extends ThrowingCallback<T, Exception> {

}
