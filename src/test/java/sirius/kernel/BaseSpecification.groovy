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
 */
class BaseSpecification extends Specification {

    /*
     * Executed before each class. Will ensure that Sirius is ready
     */

    def setupSpec() {
        TestHelper.setUp(getClass())
    }

    /*
     * Will be executed once before every spec method
     */

    def setup() {
        CallContext.initialize()
    }

    /*
     * Executed after each class ensuring that Sirius is eventually stopped
     */

    def cleanupSpec() {
        TestHelper.tearDown(getClass())
    }

}
