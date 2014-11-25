/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

/**
 * Provides tools to record the system state and to log and handle exceptions.
 * <p>
 *     The central class for handling exceptions is {@link sirius.kernel.health.Exceptions}. This provides a central
 *     facility to handle all kinds of errors. Additionally, a {@link sirius.kernel.health.HandledException} can be
 *     used to signal, that an error has already been logged and handled and needs no further attention (can be
 *     forwarded to the user).
 * </p>
 * <p>
 *     The {@link sirius.kernel.health.Log} class provides a wrapper over the currently used logging framework.
 *     Loggers are auto-configured on startup via the system configuration.
 * </p>
 * <p>
 *     Another essential framework is the {@link sirius.kernel.health.Microtiming}. This can be used for on-demand
 *     profiling (which can even be enabled in production systems). The framework gathers timing data from all kinds
 *     of sources (you can of course provide your own) and makes them available in a central place and an
 *     aggregated manner.
 * </p>
 */
package sirius.kernel.health;