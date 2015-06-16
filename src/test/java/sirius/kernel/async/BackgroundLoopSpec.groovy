/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async

import sirius.kernel.BaseSpecification
import sirius.kernel.commons.Wait

class BackgroundLoopSpec extends BaseSpecification {

    def "BackgroundLoop limits to max frequency"() {
        given:
        int currentCounter = FastTestLoop.counter;
        when:
        Wait.seconds(2);
        int delta = FastTestLoop.counter - currentCounter;
        then: "the background loop executed enough calls (should be 20 but we're a bit tolerant here)"
        delta > 18
        and: "the background loop was limited not to execute too often (should be 20 but we're a bit tolerant here)"
        delta < 22
    }

    def "BackgroundLoop executes as fast as possible if loop is slow"() {
        given:
        int currentCounter = SlowTestLoop.counter;
        when:
        Wait.seconds(2);
        int delta = SlowTestLoop.counter - currentCounter;
        then: "the background loop executed enough calls (should be 4 but we're a bit tolerant here)"
        delta >= 3
        and: "the background loop was limited not to execute too often (should be 4 but we're a bit tolerant here)"
        delta < 6
    }
}
