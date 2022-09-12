/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.timer;

import sirius.kernel.di.std.AutoRegister;

/**
 * Parts registered for this interface will be invoked every hour.
 * <p>
 * An implementing class can be inserted into the {@link sirius.kernel.di.GlobalContext} using the
 * {@link sirius.kernel.di.std.Register} annotation. Once the system is started, the method
 * {@link sirius.kernel.timer.TimedTask#runTimer()} is invoked once every hour (however no assumptions about the
 * exact length of the interval should be made - it will be "about" an hour, not exactly one).
 */
@AutoRegister
public interface EveryHour extends TimedTask {
}
