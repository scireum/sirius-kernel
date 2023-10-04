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
import java.util.List;
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
    public static LocalDateTime computeLatestDateTime(LocalDateTime... dateTimes) {
        return Dates.computeLatestDateTime(Arrays.asList(dateTimes));
    }

    /**
     * Computes the latest date-time of the given date-time objects.
     *
     * @param dateTimes a list of date-time objects to compare
     * @return the latest date-time object or <tt>null</tt> if no date is given
     */
    public static LocalDateTime computeLatestDateTime(List<LocalDateTime> dateTimes) {
        return dateTimes.stream()
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
        return computeLatestDate(Arrays.asList(dates));
    }

    /**
     * Computes the latest date of the given date objects.
     * @param dates a list of date objects to compare
     * @return the latest date object or <tt>null</tt> if no date is given
     */
    public static LocalDate computeLatestDate(List<LocalDate> dates) {
        return dates.stream()
                    .filter(Objects::nonNull)
                    .max(LocalDate::compareTo)
                    .orElse(null);
    }

    /**
     * Computes the earliest date-time of the given date-time objects.
     *
     * @param dateTimes the date-time objects to compare
     * @return the earliest date-time object or <tt>null</tt> if no date is given
     */
    public static LocalDateTime computeEarliestDateTime(LocalDateTime... dateTimes) {
        return computeEarliestDateTime(Arrays.asList(dateTimes));
    }

    /**
     * Computes the earliest date-time of the given date-time objects.
     *
     * @param dateTimes a list of date-time objects to compare
     * @return the earliest date-time object or <tt>null</tt> if no date is given
     */
    public static LocalDateTime computeEarliestDateTime(List<LocalDateTime> dateTimes) {
        return dateTimes.stream()
                        .filter(Objects::nonNull)
                        .min(LocalDateTime::compareTo)
                        .orElse(null);
    }

    /**
     * Computes the earliest date of the given date objects.
     *
     * @param dates the dates to compare
     * @return the earliest date object or <tt>null</tt> if no date is given
     */
    public static LocalDate computeEarliestDate(LocalDate... dates) {
        return computeEarliestDate(Arrays.asList(dates));
    }

    public static LocalDate computeEarliestDate(List<LocalDate> dates) {
        return dates.stream()
                    .filter(Objects::nonNull)
                    .min(LocalDate::compareTo)
                    .orElse(null);
    }
}
