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
import sirius.kernel.commons.Tuple
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

    def "removeAll works as expected"() {
        given:
        ManagedCache<String, Tuple<String, String>> cache = new ManagedCache("test-cache", null, null)
        cache.addRemover("FIRST",
                         { key, entry -> Strings.areEqual(key, entry.getValue().getFirst()) })
        cache.addRemover("SECOND",
                         { key, entry -> Strings.areEqual(key, entry.getValue().getSecond()) })

        when:
        cache.put("A", Tuple.create("0", "0"))
        cache.put("B", Tuple.create("1", "2"))
        cache.put("C", Tuple.create("2", "1"))
        cache.put("D", Tuple.create("3", "3"))
        and: "Remove all entries where the first is a '1' and then all where the second is a '1'"
        cache.removeAll("FIRST", "1")
        cache.removeAll("SECOND", "1")
        then: "Ensure that the correct entries were removed and others remained in cache"
        cache.get("A") != null
        cache.get("B") == null
        cache.get("C") == null
        cache.get("D") != null
    }
}
