/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.cache

import org.junit.jupiter.api.Tag
import sirius.kernel.BaseSpecification
import sirius.kernel.commons.Strings
import sirius.kernel.commons.Tuple
import sirius.kernel.commons.Wait

import java.util.function.BiFunction
import java.util.function.BiPredicate
import java.util.function.Function
import java.util.function.Predicate

@Tag("nightly")
class ManagedCacheSpec extends BaseSpecification {

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

    def "remover builder works as expected"() {
        given:
        ManagedCache<String, Tuple<String, String>> cache = new ManagedCache("test-cache", null, null)
        cache.addRemover("FILTER").
                filter({ selector, entry -> (entry.getKey() != selector) } as BiPredicate).
                map({ entry -> entry.getValue() } as Function).
                map({ tuple -> tuple.getFirst() } as Function).
                map({ selector, value -> value + selector } as BiFunction).
                removeIf({ x -> x.size() > 5 } as Predicate)
        cache.addValueBasedRemover("REMOVE_ALWAYS").
                removeAlways({ selector, tuple -> tuple.getSecond() == selector } as BiPredicate).
                removeIf({ false } as Predicate)
        when:
        cache.put("A", Tuple.create("gets ignored, ", "because the key is equal to the selector"))
        cache.put("B", Tuple.create("gets ", "removed, because it's too long"))
        cache.put("C", Tuple.create("does", " not get removed"))
        cache.put("D", Tuple.create("B", "A"))
        cache.put("E", Tuple.create("B", "C"))
        and:
        cache.removeAll("FILTER", "A")
        cache.removeAll("REMOVE_ALWAYS", "C")
        then:
        cache.get("A") != null
        cache.get("B") == null
        cache.get("C") != null
        cache.get("D") != null
        cache.get("E") == null
    }
}
