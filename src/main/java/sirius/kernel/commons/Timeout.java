/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import java.time.Duration;

/**
 * Wraps a timestamp into a simple predicate.
 */
public class Timeout {

    private final long timestamp;

    /**
     * Creates a new timeout which is toggled after the given amount of time.
     *
     * @param timeoutDuration the amount of time after which the timeout is reached
     */
    public Timeout(Duration timeoutDuration) {
        this.timestamp = System.currentTimeMillis() + timeoutDuration.toMillis();
    }

    /**
     * Returns <tt>true</tt> once the timeout was reached.
     *
     * @return <tt>true</tt> if the specified amount of time has elapsed since the creation of the timeout,
     * <tt>false</tt> otherwise
     */
    public boolean isReached() {
        return System.currentTimeMillis() >= timestamp;
    }

    /**
     * Returns <tt>true</tt> as long as the timeout has not been reached.
     *
     * @return <tt>true</tt> if the specified amount of time has not elapsed yet since the creation of the timeout,
     * <tt>false</tt> otherwise
     */
    public boolean notReached() {
        return System.currentTimeMillis() < timestamp;
    }
}
