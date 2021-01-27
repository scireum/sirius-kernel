/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * Provides a base implementation for {@link Spliterator spliterators} which internally pulls lists of data.
 * <p>
 * This can be used if data of unknown size is loaded "page by page" and is to be processed as a
 * {@link java.util.stream.Stream}.
 * <p>
 * Converting this into a stream is as simple as: {@code StreamSupport.stream(new MyPullBasedSpliterator(..), false)}.
 *
 * @param <T> the type of objects being processed
 */
public abstract class PullBasedSpliterator<T> implements Spliterator<T> {

    protected boolean endReached;
    protected Iterator<T> iterator;

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        if (endReached) {
            return false;
        }
        if (iterator != null && iterator.hasNext()) {
            action.accept(iterator.next());
            return true;
        }

        if (!endReached) {
            iterator = pullNextBlock();
            if (iterator == null || !iterator.hasNext()) {
                endReached = true;
            }
        }

        return tryAdvance(action);
    }

    /**
     * Pulls the next block or page of data to process.
     *
     * @return an iterator to the next page or block of items to process. May return <tt>null</tt> or
     * an "empty iterator" (where {@link Iterator#hasNext()} is <tt>false</tt>) to signal that the end of
     * the input is reached.
     */
    @Nullable
    protected abstract Iterator<T> pullNextBlock();

    @Override
    public Spliterator<T> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return 0;
    }
}
