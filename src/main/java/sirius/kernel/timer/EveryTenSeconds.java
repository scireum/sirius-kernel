/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.timer;

/**
 * Parts registered for this interface will be invoked every ten seconds.
 * <p>
 * An implementing class can be inserted into the {@link sirius.kernel.di.GlobalContext} using the
 * {@link sirius.kernel.di.std.Register} annotation. Once the system is started, the method
 * {@link TimedTask#runTimer()} is invoked once every ten seconds (however no assumptions about the
 * exact length of the interval should be made - it will be "about" ten seconds, not exactly ten).
 */
public interface EveryTenSeconds extends TimedTask {
}
