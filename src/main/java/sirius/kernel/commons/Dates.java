/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

/**
 * Provides various helper methods for dealing with dates.
 */
public class Dates {

    /**
     * All methods are static, therefore no instances are required.
     */
    private Dates() {
    }

    /**
     * Computes the latest date-time of the given date-time objects.
     *
     * @param dateTimes the date-time objects to compare
     * @return the latest date-time object or <tt>null</tt> if no date is given
     */
    public static LocalDateTime computeLatestDate(LocalDateTime... dateTimes) {
        return Arrays.stream(dateTimes)
                     .filter(Objects::nonNull)
                     .max(LocalDateTime::compareTo)
                     .orElse(null);
    }

    /**
     * Computes the latest date of the given date objects.
     *
     * @param dates the dates to compare
     * @return the latest date object or <tt>null</tt> if no date is given
     */
    public static LocalDate computeLatestDate(LocalDate... dates) {
        return Arrays.stream(dates)
                     .filter(Objects::nonNull)
                     .max(LocalDate::compareTo)
                     .orElse(null);
    }
}
