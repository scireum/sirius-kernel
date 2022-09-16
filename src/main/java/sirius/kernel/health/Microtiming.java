/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Performance measurement framework which can be used in development as well as in production systems.
 * <p>
 * Can be used to identify bottlenecks or to analyze system performance to measure the average execution time of
 * different tasks.
 * <p>
 * Once the microtiming framework is enabled it will be notified by other frameworks as defined tasks are executed.
 * The execution times are combined and can be queried using {@link sirius.kernel.health.Microtiming#getTimings()}.
 * <p>
 * An example might be an SQL query which is executed in a loop to perform a file import. Since the SQL-Query is
 * always the same (in case or a prepared statement) the execution times will be added to an average which will
 * be stored until the next call to <tt>Microtiming.getTimings()</tt>.
 */
public class Microtiming {

    private static volatile boolean enabled = false;
    private static volatile long lastReset;
    private static final Map<String, Timing> timings = new ConcurrentHashMap<>();

    private Microtiming() {
    }

    /**
     * Simple value class which represents a measured timing.
     */
    public static class Timing {

        protected String category;
        protected String key;
        protected Average avg;
        protected volatile boolean changedSinceLastCheck = false;

        protected Timing(String category, String key) {
            this.category = category;
            this.key = key;
            this.avg = new Average();
        }

        /**
         * Returns the category of the timing
         *
         * @return the category of the timing
         */
        public String getCategory() {
            return category;
        }

        /**
         * Returns the key of the timing
         *
         * @return the key of the timing
         */
        public String getKey() {
            return key;
        }

        /**
         * Returns the average duration and number of occurrences of the key.
         *
         * @return the {@link sirius.kernel.health.Average} associated with the key
         */
        public Average getAvg() {
            return avg;
        }

        /*
         * Reads and returns the changed flag, while also setting it back to false.
         */
        protected boolean readAndUnmark() {
            if (changedSinceLastCheck) {
                changedSinceLastCheck = false;
                return true;
            }
            return false;
        }

        /*
         * Adds the given duration in nanoseconds.
         * <p>
         * Also toggles the changed flag to <tt>true</tt>
         */
        protected void addNanos(long durationInNanos) {
            avg.addValue(durationInNanos / 1000);
            changedSinceLastCheck = true;
        }
    }

    /**
     * Returns a list of recorded timings.
     * <p>
     * This will only report timings, which changed since the last call of <tt>getTimings</tt>. Using this approach
     * provides a kind of auto filtering which permits to enable this framework in production systems while
     * still getting reasonable small results.
     *
     * @return all {@link Timing} values recorded which where submitted since the
     * last call to <tt>getTimings()</tt>
     */
    public static List<Timing> getTimings() {
        return timings.values().stream().filter(Timing::readAndUnmark).toList();
    }

    /**
     * Submits a new timing for the given key.
     * <p>
     * Adds the average to the "live set" which will be output on the next call to {@link #getTimings()}
     * <p>
     * A convenient way to call this method is to use {@link sirius.kernel.commons.Watch#submitMicroTiming(String, String)}
     *
     * @param category        the category of the recorded timing. This permits grouping a microtimings of the same
     *                        kind.
     * @param key             the key for which the value should be submitted
     * @param durationInNanos the number of nanoseconds used as timing for the given key
     */
    public static void submit(String category, String key, long durationInNanos) {
        if (!enabled) {
            return;
        }
        // Safety check in case someone leaves the framework enabled for a very long period of time...
        if (timings.size() > 1000) {
            timings.clear();
        }
        timings.computeIfAbsent(category + key, k -> new Timing(category, key)).addNanos(durationInNanos);
    }

    /**
     * Checks whether the timing is enabled.
     *
     * @return <tt>true</tt> if the micro timing framework is enabled or <tt>false</tt> otherwise
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables/Disables the timing.
     *
     * @param enabled determines whether the framework should be enabled or disabled
     */
    public static void setEnabled(boolean enabled) {
        if (enabled != Microtiming.enabled) {
            timings.clear();
            lastReset = System.currentTimeMillis();
        }
        Microtiming.enabled = enabled;
    }

    /**
     * Returns the timestamp of the last reset
     *
     * @return returns the timestamp in milliseconds (as provided by <tt>System.currentTimeMillis()</tt>) since the
     * last reset.
     */
    public static long getLastReset() {
        return lastReset;
    }
}
