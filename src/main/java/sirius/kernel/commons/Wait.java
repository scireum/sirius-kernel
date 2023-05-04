/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import sirius.kernel.health.Exceptions;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Random;

/**
 * Helper class for blocking the current thread for a given amount of time.
 * <p>
 * Instead of using {@link Thread#sleep(long)} there methods provide a literal feedback on the intended period. Also
 * we take care of the <tt>InterruptedException</tt> by ignoring it. This make the code more compact and readable.
 */
public class Wait {

    private static final Random rnd = new Random();

    private Wait() {
    }

    /**
     * Waits the given amount of milliseconds.
     * <p>
     * If the given value is 0 or negative, the method returns immediately. If an <tt>InterruptedException</tt>
     * is thrown while waiting, the method will return immediately but ignore the exception.
     *
     * @param millisToWait the number of milliseconds to wait
     */
    public static void millis(int millisToWait) {
        if (millisToWait > 0) {
            try {
                Thread.sleep(millisToWait);
            } catch (InterruptedException exception) {
                Exceptions.ignore(exception);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Waits the given amount of time.
     * <p>
     * If the given value is <tt>null</tt>, the method returns immediately. If an <tt>InterruptedException</tt>
     * is thrown while waiting, the method will return immediately but ignore the exception.
     *
     * @param durationToWait the duration to wait
     */
    public static void duration(@Nullable Duration durationToWait) {
        if (durationToWait != null) {
            seconds(durationToWait.getSeconds());
        }
    }

    /**
     * Waits the given amount of seconds.
     * <p>
     * If the given value is 0 or negative, the method returns immediately. If an <tt>InterruptedException</tt>
     * is thrown while waiting, the method will return immediately but ignore the exception.
     *
     * @param secondsToWait the number of seconds to wait
     */
    public static void seconds(double secondsToWait) {
        millis((int) Math.round(1000 * secondsToWait));
    }

    /**
     * Waits for a random amount of millisecond within the given bounds.
     * <p>
     * Note that minWaitMillis may be negative. This can be used to only block the thread in a given percentage of
     * all calls. So if the thread should wait between 0 and 500ms in 50% of all calls
     * {@code randomMillis(-500, 500)} can be invoked. Using this bounds, the expected wait time will be
     * between -500ms and 0ms in 50% of all calls (on average). As negative delays are ignored, the method
     * will return immediately.
     *
     * @param minWaitMillis the minimal time to wait in millis.
     * @param maxWaitMillis the maximal time to wait in millis. This must be &gt;= minWaitMillis and also &gt; 0 or the
     *                      method will always return immediately
     */
    public static void randomMillis(int minWaitMillis, int maxWaitMillis) {
        if (minWaitMillis > maxWaitMillis || maxWaitMillis < 0) {
            return;
        }
        int pauseInMillis = minWaitMillis + rnd.nextInt(maxWaitMillis - minWaitMillis);
        millis(pauseInMillis);
    }

    /**
     * Waits for a random amount of seconds within the given bounds.
     * <p>
     * See {@link #randomMillis(int, int)} for a detailed description on using a negative lower bound.
     *
     * @param minWaitSeconds the minimal time to wait in seconds.
     * @param maxWaitSeconds the maximal time to wait in seconds. This must be &gt;= minWaitSeconds and also &gt; 0
     *                       or the
     *                       method will always return immediately
     */
    public static void randomSeconds(double minWaitSeconds, double maxWaitSeconds) {
        randomMillis((int) Math.round(1000 * minWaitSeconds), (int) Math.round(1000 * maxWaitSeconds));
    }
}
