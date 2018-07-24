/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel;

import sirius.kernel.di.std.Priorized;

/**
 * Represents a rule which is executed during system statup if auto setup is enabled ({@link AutoSetup#isEnabled()}).
 */
public interface AutoSetupRule extends Priorized {

    /**
     * Executes the rule, e.g. by filling or initializing a database.
     *
     * @throws Exception in case of an error during setup
     */
    void setup() throws Exception;
}
