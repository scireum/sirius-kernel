/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

/**
 * Provides a wrapper around the <a href="https://github.com/typesafehub/config" target="_blank">typesafe config
 * library</a>.
 * <p>
 * The system configuration can be accessed using {@link sirius.kernel.Sirius#getSettings()} which returns an
 * {@link sirius.kernel.settings.ExtendedSettings} object that provides elaborate ways to utilize the system
 * configuration.
 * <p>
 * All wrappers are made available for other frameworks which operate on different configuration sources.
 */
package sirius.kernel.settings;