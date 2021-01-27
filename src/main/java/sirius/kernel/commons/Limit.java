/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import java.util.function.Predicate;

/**
 * Helper class to handle result windowing (start+limit) of arbitrary data sets.
 * <p>
 * A <tt>Limit</tt> is used to process a stream of data while limiting the results to a given window. This
 * window is described by setting the number of items to skip and the maximal number of items to pass on. If the
 * maximal number of items to pass on is <tt>null</tt>, no limiting will be performed.
 * <p>
 * This class can be used to "page" through result lists. Having a page size of 25 and showing the 2nd page,
 * would lead to:
 * <pre>
 * {@code
 *         Limit limit = new Limit(25,25); // Skip the 25 items of the first page, and show up to 25 items.
 *         List result = new ArrayList();
 *         for(Object o : someList) {
 *             if (limit.nextRow()) {
 *                 result.add(o);
 *             }
 *             // If we already fetched enough items, we can also stop processing further.
 *             if (!limit.shouldContinue()) {
 *                 return;
 *             }
 *         }
 * }
 * </pre>
 */
public class Limit {
    private int maxItems;

    private int itemsToSkip;
    private Integer itemsToOutput;

    /**
     * Represents a limit which has no upper limit and does not skip any items.
     * <p>
     * Although a limit is modified internally, we can use a constant here because a unlimited limit has
     * not internal state.
     */
    public static final Limit UNLIMITED = new Limit(0, null);

    /**
     * Creates a new {@code Limit} based on the given parameters.
     *
     * @param itemsToSkip denotes the number of items to skip.
     * @param maxItems    determines the max number of items. Can be <tt>null</tt> or 0 to indicate that no limiting
     *                    is active.
     */
    public Limit(int itemsToSkip, Integer maxItems) {
        this.itemsToSkip = Math.max(itemsToSkip, 0);
        if (maxItems != null && maxItems > 0) {
            this.itemsToOutput = maxItems;
            this.maxItems = maxItems;
        }
    }

    /**
     * Represents a limit which only accepts the first item.
     *
     * @return a new limit which will only accept the first item
     */
    public static Limit singleItem() {
        return new Limit(0, 1);
    }

    /**
     * Notifies the limit, that the next item is being processed and determines if this is part of the result.
     *
     * @return <tt>true</tt> if this item should be output, <tt>false</tt> otherwise.
     */
    public boolean nextRow() {
        if (itemsToSkip > 0) {
            itemsToSkip--;
            return false;
        }
        if (itemsToOutput != null) {
            if (itemsToOutput > 0) {
                itemsToOutput--;
                return true;
            } else {
                return false;
            }
        }

        return true;
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
     *
     * @param <C> type used by the stream using this predicate. Ignored as we do not operate on the
     *            items itself
     * @return a predicate implementing the limit
     */
    public <C> Predicate<C> asPredicate() {
        return object -> nextRow();
    }

    /**
     * Returns the max number of items or 0 to indicate that there is no upper limit.
     *
     * @return the max. number of items to accept or 0 if there is no upper limit
     */
    public int getMaxItems() {
        return maxItems;
    }

    /**
     * Returns the number of items to skip.
     * <p>
     * Note that once the limit is used, this value will change and "saturate" at 0.
     *
     * @return the number of items to skip
     */
    public int getItemsToSkip() {
        return itemsToSkip;
    }

    /**
     * Returns the total number of items to be processed. This is the number of items to be skipped plus the
     * number of items to be accepted. Returns 0 to indicate that there is no upper limit.
     *
     * @return the total number of items to be processed or 0 if there is no upper limit
     */
    public int getTotalItems() {
        return maxItems == 0 ? 0 : itemsToSkip + maxItems;
    }

    /**
     * Returns the remaining number of items to be processed. This is the number of items to be skipped plus the
     * number of items to be accepted minus the number of items already processed.
     * Returns <tt>null</tt> to indicate that there is no upper limit.
     *
     * @return the remaining number of items to be processed or <tt>null</tt> if there is no upper limit
     */
    public Integer getRemainingItems() {
        return itemsToOutput == null ? null : itemsToSkip + itemsToOutput;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Limit: ");
        if (maxItems == 0) {
            sb.append(" unlimited");
            return sb.toString();
        }
        if (itemsToSkip > 0) {
            sb.append("skip: ").append(itemsToSkip);
        }
        if (itemsToOutput == null) {
            sb.append(" output: all");
        } else {
            if (itemsToOutput > 0) {
                sb.append(" output: ").append(itemsToOutput);
            } else {
                sb.append(" output: none");
            }
        }
        sb.append(" (total: ").append(maxItems).append(")");
        return sb.toString();
    }
}
