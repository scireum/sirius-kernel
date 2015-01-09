/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Wraps an array or list of uncertain size for safe access.
 * <p>
 * Either an array or a list can be wrapped into a <b>Values</b> object. Accessing the elements of these data
 * structures is done via the <tt>at(..)</tt> methods which return the element wrapped as {@link Value}.
 * <p>
 * This permits instant coercing and conversion capabilities for the returned element and also handles
 * <tt>IndexOutOfBoundExceptions</tt> gracefully as simply an empty value is returned instead of throwing an exception.
 * <p>
 * Also this supports access via "Excel Style" column names ("A", "B", ...,  "AH") which is beneficial when importing
 * external data.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2015/01
 */
public class Values {
    private List<?> dataList;
    private Object[] dataArray;

    /*
     * Use one of the static factory methods
     */
    private Values() {

    }

    /**
     * Creates a wrapper for the given list
     *
     * @param list the list to wrap
     * @return a wrapped instance of the given list
     */
    @Nonnull
    public static Values of(@Nonnull List<?> list) {
        Objects.requireNonNull(list);

        Values result = new Values();
        result.dataList = list;

        return result;
    }

    /**
     * Creates a wrapper for the given array
     *
     * @param array the array to wrap
     * @return a wrapped instance of the given array
     */
    @Nonnull
    public static Values of(@Nonnull Object[] array) {
        Objects.requireNonNull(array);

        Values result = new Values();
        result.dataArray = array;

        return result;
    }

    /**
     * Returns the element at the (zero based) index wrapped as {@link sirius.kernel.commons.Value}
     * <p>
     * If the index is out of the valid range for the wrapped elements, an empty value will be returned
     *
     * @param index the zero based index of the element to return
     * @return the requested element wrapped as value or an empty value if no such element exists
     */
    @Nonnull
    public Value at(int index) {
        if (dataArray != null) {
            return Value.indexOf(index, dataArray);
        } else {
            return Value.indexOf(index, dataList);
        }
    }

    /**
     * Returns the element at the given column in "Excel style" notation.
     * <p>
     * Therefore <b>"A"</b> will return the first element (which is at index 0). <b>"Z"</b> will return the 26th element
     * (at index 25). <b>"AH"</b> will return the 34th element (at index 33).
     *
     * @param excelStyleIndex a column index in "Excel Notation" (A, B, ..., AA, AB, ...)
     * @return the element at the given index wrapped as value or an empty value if no such element exists
     */
    @Nonnull
    public Value at(@Nonnull String excelStyleIndex) {
        return at(convertExcelColumn(excelStyleIndex));
    }

    /**
     * Returns the number of elements in the underlying data structure
     *
     * @return the number of element wrapped
     */
    public int length() {
        if (dataArray != null) {
            return dataArray.length;
        } else {
            return dataList.size();
        }
    }

    /**
     * Converts an "Excel Style" column name into a zero based index
     * <p>
     * Therefore <b>"A"</b> will return 0. <b>"Z"</b> will return 25 and <b>AH</b> will return 33.
     *
     * @param excelStyleIndex a column index in "Excel Notation" (A, B, ..., AA, AB, ...)
     * @return the zero based index of the requested column
     */
    public static int convertExcelColumn(@Nonnull String excelStyleIndex) {
        if (Strings.isEmpty(excelStyleIndex)) {
            throw new IllegalArgumentException("excelStyleIndex must be a non-empty string like 'C'");
        }
        String input = excelStyleIndex.trim().toUpperCase();
        int result = 0;
        for (int i = 0; i < input.length(); i++) {
            char currentChar = input.charAt(i);
            if (currentChar < 'A' || currentChar > 'Z') {
                throw new IllegalArgumentException(Strings.apply(
                        "Invalid Excel style column index: %s. Character at position %s ('%s') is invalid. Only A-Z are allowed",
                        input,
                        i + 1,
                        currentChar));
            }
            result = result * 26 + (currentChar - 'A' + 1);
        }

        // Indices are zero based...
        result--;

        return result;
    }

    @Override
    public String toString() {
        if (dataArray != null) {
            return Arrays.toString(dataArray);
        } else {
            return dataList.toString();
        }
    }
}
