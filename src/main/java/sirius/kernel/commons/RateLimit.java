package sirius.kernel.commons;

import java.util.concurrent.TimeUnit;

/**
 * Limits calls to specified rate. Can be either time based, or invocation based.
 * <p>
 * This method {@link #check()} can be called frequently. It will however only return <tt>true</tt> after a certain
 * amount of time has elapsed or after a number of calls where made. The mode depends on how the object was created.
 * </p>
 * <p>
 * As an example, this can be used in an inner loop if one wants to print out the current status every once in a while.
 * Since writing to System.out is comparatively slow, it's a good idea to apply a rate limit:
 * <code>
 * <pre>
 * RateLimit limit = RateLimit.everyNthCall(1000);
 * for(int i = 0; i < 100000; i++) {
 *     // ...smart computation here...
 *     if (limit.check()) {
 *          System.out.println(i);
 *     }
 * }
 * </pre>
 * </code>
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/09
 */
public class RateLimit {


    private enum Mode {TIME_BASED, CALL_BASED}

    private long interval;
    private final Mode mode;
    private long state;

    /*
     * Use the static constructor static factory method
     */
    private RateLimit(long interval, Mode mode) {
        this.interval = interval;
        this.mode = mode;
        if (mode == Mode.CALL_BASED) {
            state = interval;
        } else {
            state = System.currentTimeMillis();
        }
    }

    /**
     * Creates a new call based rate limit.
     * <p>
     * Calling {@link #check()} on will only return <tt>true</tt> every n-th call and <tt>false</tt> otherwise.
     * </p>
     *
     * @return a new call based rate limit
     */
    public static RateLimit everyNthCall(long n) {
        return new RateLimit(n, Mode.CALL_BASED);
    }

    /**
     * Creates a new time based rate limit.
     * <p>
     * Calling {@link #check()} on will only return <tt>true</tt> every after the given amount of time has be passed
     * since the last time it returned <tt>true</tt>. Returns <tt>false</tt> otherwise.
     * </p>
     *
     * @return a new time based rate limit
     */
    public static RateLimit timeInterval(long interval, TimeUnit unit) {
        return new RateLimit(TimeUnit.MILLISECONDS.convert(interval, unit), Mode.TIME_BASED);
    }

    /**
     * Checks whether the rate limit constraints permit another call or not.
     *
     * @return <tt>true</tt> if the call or time based rate limiting permit another call, fl<tt>false</tt> otherwise
     */
    public boolean check() {
        if (mode == Mode.CALL_BASED) {
            if (--state <= 0) {
                state = interval;
                return true;
            }
        } else {
            if (System.currentTimeMillis() - state > interval) {
                state = System.currentTimeMillis();
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        if (mode == Mode.CALL_BASED) {
            return Strings.apply("Every %d calls: %d to go...", interval, state);
        }
        long delta =  interval - (System.currentTimeMillis() - state);
        if (delta > 0) {
            return Strings.apply("Every %d ms: %d ms to go...", interval,delta);
        } else {
            return Strings.apply("Every %d ms: Ready to go...", interval);
        }
    }
}
