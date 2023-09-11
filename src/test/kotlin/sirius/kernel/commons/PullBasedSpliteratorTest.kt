/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */
package sirius.kernel.commons

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream
import java.util.stream.StreamSupport

internal class PullBasedSpliteratorTest {
    private class TestSpliterator : PullBasedSpliterator<Int?>() {
        private var index = 0
        override fun pullNextBlock(): Iterator<Int>? {
            if (index < 10) {
                val result: MutableList<Int> = ArrayList()
                for (i in index until index + 5) {
                    result.add(i)
                }
                index += 5
                return result.iterator()
            }
            return Collections.emptyIterator()
        }

        override fun characteristics(): Int {
            return 0
        }
    }

    @Test
    fun streamWorksProperly() {
        Assertions.assertEquals(10, StreamSupport.stream(TestSpliterator(), false).count())
        Assertions.assertEquals(
            StreamSupport.stream(TestSpliterator(), false).collect(Collectors.toList()),
            Stream.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).collect(Collectors.toList())
        )
    }
}