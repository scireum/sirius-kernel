package sirius.kernel.health

import sirius.kernel.BaseSpecification

class ExceptionsSpec extends BaseSpecification {

    def "the root cause"() {
        when:
        def root = Exceptions.createHandled().withSystemErrorMessage("Root Cause").handle()
        def e = new IllegalArgumentException(root)
        e = new Exception(e)
        e = Exceptions.handle(e)
        then:
        Exceptions.getRootCause(e) instanceof HandledException
        and:
        Exceptions.getRootCause(e) == root
    }
}
