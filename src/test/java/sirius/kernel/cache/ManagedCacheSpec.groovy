/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.cache

import sirius.kernel.BaseSpecification
import sirius.kernel.Scope
import sirius.kernel.commons.Strings
import sirius.kernel.commons.Wait

class ManagedCacheSpec extends BaseSpecification {

    @Scope(Scope.SCOPE_NIGHTLY)
    def "test run eviction removes old entries"() {
        given:
        def cache = new ManagedCache("test-cache", null, null)
        when:
        cache.put("key1", "value1")
        cache.put("key2", "value2")
        Wait.millis(1001)
        cache.put("key3", "value3")
        cache.put("key4", "value4")
        then:
        cache.getSize() == 4
        cache.runEviction()
        cache.getSize() == 2
        cache.get("key3") == "value3"
        cache.get("key4") == "value4"
    }

    def "optional value computer works"() {
        given:
        def valueComputer = { key ->
            if (Strings.isEmpty(key)) {
                return Optional.empty()
            }
            if (key.startsWith("empty")) {
                return Optional.empty()
            } else {
                return Optional.of(key.toUpperCase())
            }
        }
        def cache = new ManagedCache("test-cache", OptionalValueComputer.of(valueComputer), null)
        expect:
        cache.get("") == null
        cache.getOptional("key") == Optional.of("KEY")
        cache.getOptional("empty_key") == Optional.empty()
    }
}
