/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

/**
 * Provides a way of intercepting background tasks like {@link BackgroundLoop} and {@link sirius.kernel.timer.EveryDay}
 * to synchronize those activities across a cluster.
 */
public interface Orchestration {

    /**
     * Determines if the background loop with the given name should be executed.
     *
     * @param name the name of the loop
     * @return <tt>true</tt> if the loop should be executed on this node, <tt>false</tt> otherwise
     */
    boolean tryExecuteBackgroundLoop(String name);

    /**
     * Invoked once a {@link BackgroundLoop} completed it's work.
     *
     * @param name          the name of the background loop
     * @param executionInfo some info about the last execution
     */
    void backgroundLoopCompleted(String name, String executionInfo);

    /**
     * Determines if a daily task should be executed.
     *
     * @param name the name of the task
     * @return <tt>true</tt> if the task should be executed, <tt>false</tt> otherwise
     */
    boolean shouldRunDailyTask(String name);
}
