/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

/**
 * Support for executing tasks in regular intervals.
 * <p>
 * Provides a {@link sirius.kernel.timer.Timers} which executes all parts in the
 * {@link sirius.kernel.di.GlobalContext}, registered for one of the timer interfaces (<tt>EveryMinute</tt>,
 * <tt>EveryTenMinutes</tt>, <tt>EveryHour</tt>, <tt>EveryDay</tt>) in their appropriate interval.
 * <p>
 * As this framework is based on the dependency injection framework, the classes only need to implement the
 * respective interface and a {@link sirius.kernel.di.std.Register} annotation to be executed. The
 * <tt>TimerService</tt> is only accessed for maintenance or statistical reasons.
 */
package sirius.kernel.timer;