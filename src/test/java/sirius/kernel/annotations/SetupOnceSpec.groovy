/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.annotations

import sirius.kernel.BaseSpecification
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Test that setup once Annotation is only invoked once in data driven tests 
 */
class SetupOnceSpec extends BaseSpecification {
    
    @Shared
    int counter

    def setupSpec() {
        counter = 0
    }
    
    void setupOnceMethod() {
        counter = 20
    }

    @SetupOnce('setupOnceMethod')
    def 'setup once method is only invoked once'() {
        setup:
        counter--
        //invoked once for each data set
        expect:
        counter == x
        where:
        x << [19, 18, 17, 16, 15, 14, 13, 12, 11, 10]
    }

    @SetupOnce('setupOnceMethod')
    @Unroll
    def 'setup once method is only invoked once in unrolled methods #x'() {
        setup:
        counter--
        //invoked once for each data set
        expect:
        counter == x
        where:
        x << [19, 18, 17, 16, 15, 14, 13, 12, 11, 10]
    }
}
