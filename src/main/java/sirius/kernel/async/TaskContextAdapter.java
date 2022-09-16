/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

import sirius.kernel.commons.RateLimit;

import java.util.function.Supplier;

/**
 * Implementations of this interface can be attached to a {@link sirius.kernel.async.TaskContext} of a thread to
 * perform
 * monitoring and logging.
 */
public interface TaskContextAdapter {
    /**
     * Invoked if {@link sirius.kernel.async.TaskContext#log(String, Object...)} is called in the attached context.
     *
     * @param message the message to log
     */
    void log(String message);

    /**
     * Invoked if {@link sirius.kernel.async.TaskContext#trace(String, Object...)} is called in the attached context.
     *
     * @param message the message to log
     */
    void trace(String message);

    /**
     * Invoked if {@link TaskContext#shouldUpdateState()} is called in the attached context.
     *
     * @return a rate limit which limits the number of updates to a reasonable value
     */
    RateLimit shouldUpdateState();

    /**
     * Invoked if {@link sirius.kernel.async.TaskContext#tryUpdateState(String, Object...)} is called in the attached
     * context.
     *
     * @param message the message to set as state
     */
    void tryUpdateState(String message);

    /**
     * Invoked if {@link sirius.kernel.async.TaskContext#forceUpdateState(String, Object...)} is called in the attached
     * context.
     *
     * @param message the message to set as state
     */
    void forceUpdateState(String message);

    /**
     * Invoked if {@link sirius.kernel.async.TaskContext#logLimited(Object)} is called in the attached context.
     *
     * @param message the message to add to the logs.
     */
    void logLimited(Object message);

    /**
     * Invoked if {@link sirius.kernel.async.TaskContext#smartLogLimited(Supplier)} is called in the attached context.
     *
     * @param messageSupplier the supplier which yields the message to log on demand
     * @see #logLimited(Object)
     */
    void smartLogLimited(Supplier<Object> messageSupplier);

    /**
     * Invoked if {@link sirius.kernel.async.TaskContext#addTiming(String, long)} is called in the attached context.
     *
     * @param counter the counter to increment
     * @param millis  the current duration for the block being counted
     */
    void addTiming(String counter, long millis);

    /**
     * Invoked if {@link sirius.kernel.async.TaskContext#addTiming(String, long, boolean)} is called in the attached context.
     *
     * @param counter   the counter to increment
     * @param millis    the current duration for the block being counted
     * @param adminOnly whether to show the timing only to administrators instead of all users
     */
    void addTiming(String counter, long millis, boolean adminOnly);

    /**
     * Invoked if {@link TaskContext#markErroneous()} is called in the attached context.
     */
    void markErroneous();

    /**
     * Invoked if {@link sirius.kernel.async.TaskContext#cancel()} is called in the attached context.
     */
    void cancel();

    /**
     * Determines if the current task is still "active" and processing should continue.
     *
     * @return <tt>true</tt> if execution should be continued, <tt>false</tt> otherwise
     */
    boolean isActive();
}
