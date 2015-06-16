/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.timer;

/**
 * Super-interface for timer intervals.
 * <p>
 * See subclasses for defined semantics.
 */
public interface TimedTask {

    /**
     * Called every time the timer interval is fired.
     * <p>
     * Note that only small computations can be done in this method. Computations which take longs must
     * be executed in another executor as otherwise the timer will run out of schedule and start dropping tasks.
     *
     * @throws Exception in case anything goes wrong. Will be caught and handled using
     *                   {@link sirius.kernel.health.Exceptions}
     * @see sirius.kernel.async.Tasks#executor(String)
     */
    void runTimer() throws Exception;
}