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
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public interface TimedTask {

    /**
     * Called every time the timer interval is fired.
     *
     * @throws Exception in case anything goes wrong. Will be caught and handled using
     *                   {@link sirius.kernel.health.Exceptions}
     */
    void runTimer() throws Exception;

}