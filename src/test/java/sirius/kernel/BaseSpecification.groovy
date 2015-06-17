/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel

import sirius.kernel.async.CallContext
import spock.lang.Specification

/**
 * Base class for all specs that require the Sirius framework to be setup.
 * Extending specifications can be run from their respective Suites or stand alone without any additional configuration.
 */
class BaseSpecification extends Specification {

    /**
     * Indicates whether this class initially started the framework (and thus is responsible for shutting it down again).
     */
    static Class initialFrameworkStarter = null;


    /**
     * Will be executed once before every spec class.
     * If Sirius is not yet started, this will take care of that and mark itself as the {@link #initialFrameworkStarter}
     */
    def setupSpec() {
        if (!Sirius.startedAsTest) {
            initialFrameworkStarter = getClass()
            TestHelper.setUp();
        }
    }

    /**
     * Will be executed once before every spec method
     */
    def setup() {
        CallContext.initialize()
    }

    /**
     * Will be called once after every spec class.
     * if this spec is the {@link #initialFrameworkStarter}, this will handle shutdown.
     */
    def cleanupSpec() {
        if (initialFrameworkStarter == getClass()) {
            TestHelper.tearDown();
        }
    }

}
