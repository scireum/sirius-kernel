/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import com.google.common.collect.Range;

import javax.annotation.Nonnull;
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
        return dateTimes.stream().filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(null);
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
     *
     * @param dates a list of date objects to compare
     * @return the latest date object or <tt>null</tt> if no date is given
     */
    public static LocalDate computeLatestDate(List<LocalDate> dates) {
        return dates.stream().filter(Objects::nonNull).max(LocalDate::compareTo).orElse(null);
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
        return dateTimes.stream().filter(Objects::nonNull).min(LocalDateTime::compareTo).orElse(null);
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

    /**
     * Computes the earliest date of the given date objects.
     *
     * @param dates a list of date objects to compare
     * @return the earliest date object or <tt>null</tt> if no date is given
     */
    public static LocalDate computeEarliestDate(List<LocalDate> dates) {
        return dates.stream().filter(Objects::nonNull).min(LocalDate::compareTo).orElse(null);
    }

    /**
     * Determines if the given date is before or after the given reference date.
     *
     * @param dateToCheck   the date to check
     * @param referenceDate the reference date
     * @return <tt>true</tt> if the date is before or after the reference date, <tt>false</tt> otherwise
     */
    public static boolean isBeforeOrEqual(@Nonnull LocalDate dateToCheck, @Nonnull LocalDate referenceDate) {
        return Range.atMost(referenceDate).contains(dateToCheck);
    }

    /**
     * Determines if the given date is after or equal to the given reference date.
     *
     * @param dateToCheck   the date to check
     * @param referenceDate the reference date
     * @return <tt>true</tt> if the date is after or equal to the reference date, <tt>false</tt> otherwise
     */
    public static boolean isAfterOrEqual(@Nonnull LocalDate dateToCheck, @Nonnull LocalDate referenceDate) {
        return Range.atLeast(referenceDate).contains(dateToCheck);
    }

    /**
     * Determines if the given date is within the given range.
     *
     * @param startDate   the range's start date
     * @param endDate     the range's end date
     * @param dateToCheck the date to check
     * @return <tt>true</tt> if the date is within the range, <tt>false</tt> otherwise
     */
    public static boolean isWithinRange(@Nonnull LocalDate startDate,
                                        @Nonnull LocalDate endDate,
                                        @Nonnull LocalDate dateToCheck) {
        return Range.closed(startDate, endDate).contains(dateToCheck);
    }
}
