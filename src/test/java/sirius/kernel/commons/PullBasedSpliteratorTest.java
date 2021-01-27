/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class PullBasedSpliteratorTest {

    private static class TestSpliterator extends PullBasedSpliterator<Integer> {

        private int index;

        @Nullable
        @Override
        protected Iterator<Integer> pullNextBlock() {
            if (index < 10) {
                List<Integer> result = new ArrayList<>();
                for (int i = index; i < index + 5; i++) {
                    result.add(i);
                }
                index += 5;

                return result.iterator();
            }

            return Collections.emptyIterator();
        }

        @Override
        public int characteristics() {
            return 0;
        }
    }

    @Test
    public void streamWorksProperly() {
        Assert.assertEquals(StreamSupport.stream(new TestSpliterator(), false).count(), 10);
        Assert.assertEquals(StreamSupport.stream(new TestSpliterator(), false).collect(Collectors.toList()),
                            Stream.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).collect(Collectors.toList()));
    }
}
