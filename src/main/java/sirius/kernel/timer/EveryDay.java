/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.timer;

/**
 * Parts registered for this interface will be invoked one time per day.
 * <p>
 * An implementing class can be inserted into the {@link sirius.kernel.di.GlobalContext} using the
 * {@link sirius.kernel.di.std.Register} annotation. Once the system is started, the method
 * {@link sirius.kernel.timer.TimedTask#runTimer()} is invoked once per day. The hour within the call will take place
 * is determined by the config value named in {@link #getConfigKeyName()}. (The full path in the config
 * will be <tt>timer.daily.[getConfigKeyName]</tt>).
 * <p>
 * The call interval will not be exactly one day, as the time of invocation will vary from day to day. The system
 * ensures, that the method is called within the given hour of day, and not executed more than once per day. However,
 * if the system is restarted within the given hour, the method might be called twice, as the time of call is not
 * persisted over restarts. If a more precise behaviour is required, the subclass must take care of handling such
 * cases.
 */
public interface EveryDay extends TimedTask {

    /**
     * Returns the name of the config key, which stores the hour of execution.
     *
     * @return the config key to determine the hour if execution
     */
    String getConfigKeyName();
}
