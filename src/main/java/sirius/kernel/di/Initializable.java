/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di;

import sirius.kernel.di.std.AutoRegister;

/**
 * Classes implementing this interface will be invoked, once the {@link Injector} is fully initialized (all annotations
 * are processed).
 */
@AutoRegister
public interface Initializable {
    /**
     * Invoked by the injector once the system is completely initialized.
     * <p>
     * Can be used to perform initial actions where access to dependent parts is required.
     *
     * @throws Exception in case of any error during the initialization.
     */
    void initialize() throws Exception;
}
