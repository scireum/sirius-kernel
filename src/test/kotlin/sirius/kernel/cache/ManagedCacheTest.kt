/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.cache

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import sirius.kernel.SiriusExtension
import sirius.kernel.commons.Strings
import sirius.kernel.commons.Tuple
import sirius.kernel.commons.Wait
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Tests the [AdvancedDateParser] class.
 */
@ExtendWith(SiriusExtension::class)
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
                Optional.of(key.toUpperCase())
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
        `Remove all entries where the first is a '1' and then all where the second is a '1'`(cache)
        `Ensure that the correct entries were removed and others remained in cache`(cache)
    }

    @DisplayName("Remove all entries where the first is a '1' and then all where the second is a '1'")
    private fun `Remove all entries where the first is a '1' and then all where the second is a '1'`(cache: ManagedCache<String, Tuple<String, String>>) {
        cache.removeAll("FIRST", "1")
        cache.removeAll("SECOND", "1")
    }

    @DisplayName("Ensure that the correct entries were removed and others remained in cache")
    private fun `Ensure that the correct entries were removed and others remained in cache`(cache: ManagedCache<String, Tuple<String, String>>) {
        assertNotEquals(null, cache.get("A"))
        assertEquals(null, cache.get("B"))
        assertEquals(null, cache.get("C"))
        assertNotEquals(null, cache.get("D"))
    }

    @Test
    fun `remover builder works as expected`() {
        val cache: ManagedCache<String, Tuple<String, String>> = ManagedCache("test-cache", null, null)
        cache.put("A", Tuple.create("gets ignored, ", "because the key is equal to the selector"))
        cache.put("B", Tuple.create("gets ", "removed, because it's too long"))
        cache.put("C", Tuple.create("does", " not get removed"))
        cache.put("D", Tuple.create("B", "A"))
        cache.put("E", Tuple.create("B", "C"))

        // defines a remover, that allows to define a key value, which should not be removed
        cache.addRemover("FILTER")
            .filter { selector: String, entry: CacheEntry<String, Tuple<String, String>> ->
                (entry.getKey() != selector)
            }
            // get tuple consisting of two strings
            .map { entry: CacheEntry<String, Tuple<String, String>?> ->
                entry.getValue()
            }
            .map { tuple: Tuple<String, String>? ->
                // get first value of tuple (which is a string)
                tuple?.first
            }.map(
                // add the selector (key of the cache entry) and the first value of the string
                // for the cache value B this would be: "B" + "gets " = "Bgets "
                { selector, value ->
                    value + selector
                }
            ).removeIf(
                // if the resulting string, f.E. "Bgets " is larger than 5, remove entry
                { x ->
                    x.length > 5
                }
            )

        cache.addValueBasedRemover("REMOVE_ALWAYS").removeAlways({ selector, tuple ->
            tuple.getSecond() == selector
        }).removeIf({ tuple ->
            false
        })

        cache.removeAll("FILTER", "A")
        cache.removeAll("REMOVE_ALWAYS", "C")

        assertNotEquals(null, cache.get("A"))
        assertEquals(null, cache.get("B"))
        assertNotEquals(null, cache.get("C"))
        assertNotEquals(null, cache.get("D"))
        assertEquals(null, cache.get("E"))
    }
}
