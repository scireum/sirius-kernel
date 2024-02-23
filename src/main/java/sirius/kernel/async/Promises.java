/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Provides utility functions when dealing with {@link Promise promises}.
 */
public class Promises {

    private Promises() {
    }

    /**
     * Iterates over the given input and invokes an async function for each element.
     * <p>
     * Note that this is done sequentially, therefore only one call happens at a time
     *
     * @param items            the items to iterate over
     * @param toPromiseHandler the async function which returns a promise to indicate completion
     * @param resultConsumer   the handler used to collect the results
     * @param <I>              the input type
     * @param <O>              the output type generated by the async function
     * @return a future which is fulfilled once all items have been processer or failed if one item fails
     */
    public static <I, O> Future processChain(Iterable<I> items,
                                             Function<I, Promise<O>> toPromiseHandler,
                                             BiConsumer<I, O> resultConsumer) {
        Future result = new Future();
        processChain(items.iterator(), toPromiseHandler, resultConsumer, result);

        return result;
    }

    private static <I, O> void processChain(Iterator<I> iter,
                                            Function<I, Promise<O>> toPromiseHandler,
                                            BiConsumer<I, O> resultConsumer,
                                            Future completionFuture) {
        if (!iter.hasNext()) {
            completionFuture.success();
            return;
        }

        I item = iter.next();
        toPromiseHandler.apply(item).onSuccess(result -> {
            resultConsumer.accept(item, result);
            processChain(iter, toPromiseHandler, resultConsumer, completionFuture);
        }).onFailure(completionFuture::fail);
    }

    /**
     * Transforms a collection of items into a promise for a list of results while invoking an async function for
     * each item.
     *
     * @param input            the items to iterate over
     * @param toPromiseHandler the async function which returns a promise to indicate completion
     * @param <I>              the input type
     * @param <O>              the output type generated by the async function
     * @return a promise containing the invocation results of the async function for each item in the input
     */
    public static <I, O> Promise<List<O>> sequence(Iterable<I> input, Function<I, Promise<O>> toPromiseHandler) {
        Promise<List<O>> result = new Promise<>();
        List<O> buffer = new ArrayList<>();
        processChain(input, toPromiseHandler, (ignored, output) -> buffer.add(output)).onSuccess(() -> result.success(
                buffer)).onFailure(result::fail);

        return result;
    }

    /**
     * Turns a list of promises into a promise for a list of values.
     * <p>
     * Note that all values need to have the same type.
     * <p>
     * If only the completion of all promises matters in contrast to their actual result, a {@link CombinedFuture} can also
     * be used. This also permits waiting for promises of different types.
     *
     * @param list the list of promises to convert.
     * @param <V>  the type of each promise.
     * @return the promise which will complete if all promises completed or if at least on failed.
     */
    public static <V> Promise<List<V>> parallel(List<Promise<V>> list) {
        final Promise<List<V>> result = new Promise<>();

        if (list.isEmpty()) {
            result.success(Collections.emptyList());
            return result;
        }

        if (list.size() == 1) {
            return list.getFirst().map(Collections::singletonList);
        }

        // Create a list with the correct length
        final List<V> resultList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            resultList.add(null);
        }

        // Keep track when we're finished
        final CountDownLatch latch = new CountDownLatch(list.size());

        // Iterate over all promises and create a completion handler, which either forwards a failure or which places
        // a successfully computed value in the created result list
        int index = 0;
        for (Promise<V> promise : list) {
            final int currentIndex = index;
            promise.onComplete(new CompletionHandler<V>() {
                @Override
                public void onSuccess(@Nullable V value) throws Exception {
                    if (!result.isFailed()) {
                        // onSuccess can be called from any thread -> sync on resultList...
                        synchronized (resultList) {
                            resultList.set(currentIndex, value);
                        }

                        // Keep track how many results we're waiting for and forward the result when we're finished.
                        latch.countDown();
                        if (latch.getCount() <= 0) {
                            result.success(resultList);
                        }
                    }
                }

                @Override
                public void onFailure(Throwable throwable) throws Exception {
                    result.fail(throwable);
                }
            });
            index++;
        }

        return result;
    }
}
