/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.cache

import sirius.kernel.BaseSpecification
import sirius.kernel.health.HandledException

class SizedCacheSpec extends BaseSpecification {

    def valuecomputer = { key ->
        return key.replace("key", "value")
    }

    def "contains key works"() {
        given:
        def cache = new SizedCache(2)

        when:
        cache.put("key1", "value1")
        cache.put("key2", "value2")
        cache.put("key3", "value3")

        then:
        cache.containsKey("key0") == false
        cache.containsKey("key1") == false
        cache.containsKey("key2") == true
        cache.containsKey("key3") == true
    }

    def "get without computer works"() {
        given:
        def cache = new SizedCache(2)

        when:
        cache.put("key1", "value1")
        cache.get("key1")

        then:
        thrown IllegalStateException
    }

    def "get with default computer works"() {
        when:
        def cache = new SizedCache(2, valuecomputer)

        then:
        cache.get("key1") == "value1"
        cache.get("key2") == "value2"
        cache.get("key3") == "value3"
        cache.containsKey("key1") == false
    }

    def "get with inline computer works"() {
        when:
        def cache = new SizedCache(2)

        then:
        cache.get("key1", valuecomputer) == "value1"
        cache.get("key2", valuecomputer) == "value2"
        cache.get("key3", valuecomputer) == "value3"
        cache.containsKey("key1") == false
    }

    def "init cache size from config works"() {
        when:
        def cache = new SizedCache("cache.invalid-cache-key")

        then:
        thrown HandledException

        when:
        cache = new SizedCache("cache.test-sized-cache", valuecomputer)

        then:
        cache.get("key1") == "value1"
        cache.get("key2") == "value2"
        cache.get("key3") == "value3"
        cache.containsKey("key1") == false
    }

    def "cache cleanup works"() {
        when:
        def cache = new SizedCache(2)
        cache.put("key1", "value1")

        then:
        cache.containsKey("key1") == true
        cache.clear()
        cache.containsKey("key1") == false
    }
}
