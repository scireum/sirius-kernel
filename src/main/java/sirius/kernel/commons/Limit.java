package sirius.kernel.commons;

import java.util.function.Predicate;

/**
 * Helper class to handle result windowing (start+limit) of arbitrary data sets.
 * <p>
 * A <tt>Limit</tt> is used to process a stream of data while limiting the results to a given window. This
 * window is described by setting the number of items to skip and the maximal number of items to pass on. If the
 * maximal number of items to pass on is <tt>null</tt>, no limiting will be performed.
 * </p>
 * <p>
 * This class can be used to "page" through result lists. Having a page size of 25 and showing the 2nd page,
 * would lead to:
 * <code>
 * <pre>
 *         Limit limit = new Limit(25,25); // Skip the 25 items of the first page, and show up to 25 items.
 *         List result = new ArrayList();
 *         for(Object o : someList) {
 *             limit.nextRow();
 *             if (limit.shouldOutput()) {
 *                 result.add(o);
 *             }
 *             // If we already fetched enough items, we can also stop processing further.
 *             if (!limit.shouldContinue()) {
 *                 return;
 *             }
 *         }
 * </pre>
 * </code>
 * </p>
 */
public class Limit {
    private int itemsToSkip;
    private Integer itemsToOutput;

    /**
     * Creates a new <code>Limit</code> based on the given parameters.
     *
     * @param itemsToSkip denotes the number of items to skip.
     * @param maxItems    determines the max number of items. Can be <tt>null</tt> or 0 to indicate that no limiting is active.
     */
    public Limit(int itemsToSkip, Integer maxItems) {
        this.itemsToSkip = itemsToSkip;
        if (maxItems != null && maxItems != 0) {
            itemsToOutput = maxItems + 1;
        }
    }

    /**
     * Notifies the limit, that the next row is being processed.
     * <p>
     * Must be called before any call to {@link #shouldOutput()} or {@link #shouldContinue()}.
     * </p>
     */
    public void nextRow() {
        if (itemsToSkip > 0) {
            itemsToSkip--;
        }
        if (itemsToOutput != null) {
            itemsToOutput--;
        }
    }

    /**
     * Determines if  the current row should be passed on.
     *
     * @return <tt>true</tt> if the current row is accepted as output, <tt>false</tt> otherwise
     */
    public boolean shouldOutput() {
        return itemsToSkip == 0 && itemsToOutput == null || itemsToOutput > 0;
    }

    /**
     * Determines if already enough items have been processed or if processing should continue.
     *
     * @return <tt>true</tt> if more items can be used as output, <tt>false</tt> otherwise
     */
    public boolean shouldContinue() {
        return itemsToOutput == null || itemsToOutput > 0;
    }

    /**
     * Converts the limit into a predicate.
     * <p>
     * Note that the limit is stateful and therefore asPredicate should only be called once.
     * </p>
     *
     * @return a predicate implementing the limit
     */
    public Predicate<?> asPredicate() {
        return object -> {
            if (shouldOutput()) {
                nextRow();
                return true;
            }
            return false;
        };
    }

}
