/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

/**
 * Provides a framework for asynchronous execution of given tasks.
 * <p>
 * Most of the functionality is accessed via the {@link sirius.kernel.async.Tasks} class. This can be used to
 * submit tasks to executors which are configured via the extension <tt>async.executor</tt>.
 * <p>
 * Another central class is the {@link sirius.kernel.async.CallContext}. This is attached to a thread and will be
 * passed along to subtasks. This can be used to provide implicit information like the current user for all
 * subsequent actions of a task. Also this contains the mapped diagnostic context (MDC) which can be useful
 * for debugging and logging, as it reveals the current context and flow of execution through several threads.
 * <p>
 * Also, {@link sirius.kernel.async.Promise} is a class representing a central concept. A promise can be returned
 * by a call which internally starts a computation which does not immediately complete. The returned promise can
 * be passed on or have completion handlers attached. This permits a non-block way of interacting between several
 * threads and system components. The class {@link sirius.kernel.async.Future} is basically an untyped promise and
 * can be used to wait for the completion of a task which has no immediate return value.
 */
package sirius.kernel.async;