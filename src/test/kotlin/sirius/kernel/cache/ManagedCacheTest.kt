/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.cache

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import sirius.kernel.NightlyTest
import sirius.kernel.SiriusExtension
import sirius.kernel.commons.Strings
import sirius.kernel.commons.Tuple
import sirius.kernel.commons.Wait
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Tests the [ManagedCache] class.
 */
@ExtendWith(SiriusExtension::class)
@NightlyTest
class ManagedCacheTest {

    @Test
    fun `test run eviction removes old entries`() {
        val cache: ManagedCache<String, String> = ManagedCache("test-cache", null, null)
        cache.put("key1", "value1")
        cache.put("key2", "value2")
        Wait.millis(1001)
        cache.put("key3", "value3")
        cache.put("key4", "value4")
        assertEquals(4, cache.getSize())
        cache.runEviction()
        assertEquals(2, cache.getSize())
        assertEquals("value3", cache.get("key3"))
        assertEquals("value4", cache.get("key4"))
    }

    @Test
    fun `optional value computer works`() {
        val valueComputer = { key: String ->
            if (key.isEmpty() || key.startsWith("empty")) {
                Optional.empty<String>()
            } else {
                Optional.of(key.uppercase())
            }
        }
        val cache = ManagedCache("test-cache", OptionalValueComputer.of(valueComputer), null)
        assertEquals(null, cache.get(""))
        assertEquals(Optional.of("KEY"), cache.getOptional("key"))
        assertEquals(Optional.empty(), cache.getOptional("empty_key"))

    }

    @Test
    fun `removeAll works as expected`() {
        val cache: ManagedCache<String, Tuple<String, String>> = ManagedCache("test-cache", null, null)
        cache.addRemover("FIRST",
                { key, entry ->
                    Strings.areEqual(key, entry.getValue()?.getFirst())
                })
        cache.addRemover("SECOND",
                { key, entry ->
                    Strings.areEqual(key, entry.getValue()?.getSecond())
                })
        cache.put("A", Tuple.create("0", "0"))
        cache.put("B", Tuple.create("1", "2"))
        cache.put("C", Tuple.create("2", "1"))
        cache.put("D", Tuple.create("3", "3"))

        //Remove all entries where the first is a '1' and then all where the second is a '1'
        cache.removeAll("FIRST", "1")
        cache.removeAll("SECOND", "1")

        //Ensure that the correct entries were removed and others remained in cache
        assertNotEquals(null, cache.get("A"))
        assertEquals(null, cache.get("B"))
        assertEquals(null, cache.get("C"))
        assertNotEquals(null, cache.get("D"))
    }

    @Test
    fun `remover builder works as expected`() {
        val cache: ManagedCache<String, String> = ManagedCache("test-cache", null, null)
        cache.put("A", "1")
        cache.put("B", "12")
        cache.put("C", "123")
        cache.put("D", "1234")
        cache.put("E", "12345")

        // defines a remover, that allows to define a key value, which should not be removed
        cache.addRemover("FILTER")
                .filter(
                        { selector, entry ->
                            entry.getKey() != selector
                        }
                )
                .map(
                        // get value of managed cache
                        { entry ->
                            entry.getValue()
                        }
                )
                .map(
                        // add key + value of the cache Entry, for example: "E" + 12345
                        { selector, value ->
                            value + selector
                        }
                ).removeIf(
                        // if the resulting string, f.E. "E12345" is larger than 5, remove entry
                        { x ->
                            x.length > 5
                        }
                )

        cache.removeAll("FILTER", "A")

        assertNotEquals(null, cache.get("A"))
        assertNotEquals(null, cache.get("B"))
        assertNotEquals(null, cache.get("C"))
        assertNotEquals(null, cache.get("D"))
        assertEquals(null, cache.get("E"))
    }

    @Test
    fun `valueBasedRemover works as expected`() {
        val cache: ManagedCache<String, Tuple<String, String>> = ManagedCache("test-cache", null, null)
        cache.put("Key1", Tuple.create("1, ", "A"))
        cache.put("Key2", Tuple.create("2 ", "B"))
        cache.put("Key3", Tuple.create("3", "C"))
        cache.put("Key4", Tuple.create("4", "D"))
        cache.put("Key5", Tuple.create("5", "E"))

        cache.addValueBasedRemover("REMOVE_ALWAYS")
                .removeAlways({ selector, tuple ->
                    tuple.getSecond() == selector
                }).removeIf({ _ ->
                    false
                })

        cache.removeAll("REMOVE_ALWAYS", "C")
        cache.removeAll("REMOVE_ALWAYS", "B")

        assertNotEquals(null, cache.get("Key1"))
        assertEquals(null, cache.get("Key2"))
        assertEquals(null, cache.get("Key3"))
        assertNotEquals(null, cache.get("Key4"))
        assertNotEquals(null, cache.get("Key5"))
    }

    @Test
    fun `a removal listener is correctly invoked upon eviction`() {
        var invoked = false
        val cache: ManagedCache<String, String> = ManagedCache("test-cache", null, null)
        cache.onRemove { keyValueTuple ->
            assertEquals("key1", keyValueTuple.getFirst())
            assertEquals("value1", keyValueTuple.getSecond())
            invoked = true
        }
        cache.put("key1", "value1")
        cache.remove("key1")

        // wait a bit so that the listener can be invoked before we check the invocation flag
        Wait.millis(101)
        assert(invoked) { "The removal listener was not invoked!" }
    }
}
