/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.event

import sirius.kernel.BaseSpecification
import sirius.kernel.di.std.Part


class FireEventSpec extends BaseSpecification {

    @Part
    private static EventBus eventBus;

    def "Fire matching event"() {
        given:
        TestEventHandler.testString = "String-call"
        and:
        "String-call".equals(TestEventHandler.testString)
        when:
        eventBus.fireEvent("String-call", "String-call_completed")
        then: "Event should be handled"
        "String-call_completed".equals(TestEventHandler.testString)
    }

    def "Fire non-matching event"() {
        given:
        TestEventHandler.testString = "String-call"
        and:
        "String-call".equals(TestEventHandler.testString)
        when:
        eventBus.fireEvent("non-String-call", "non-String-call_completed")
        then: "Event should not be handled"
        "String-call".equals(TestEventHandler.testString)
    }

}
