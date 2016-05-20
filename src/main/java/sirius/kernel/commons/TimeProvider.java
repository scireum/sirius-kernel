/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import sirius.kernel.di.std.Register;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Wrapper for static time functions which can be injected using a {@link sirius.kernel.di.std.Part} annotation.
 * <p>
 * Using this wrapper instead of the static methods permits to use mocking in tests.
 */
@Register(classes = TimeProvider.class)
public class TimeProvider {

    /**
     * Wrapper for {@link LocalDateTime#now()}.
     *
     * @return the current time as {@link LocalDateTime}.
     */
    public LocalDateTime localDateTimeNow() {
        return LocalDateTime.now();
    }

    /**
     * Wrapper for {@link LocalDate#now()}.
     *
     * @return the current time as {@link LocalDate}.
     */
    public LocalDate localDateNow() {
        return LocalDate.now();
    }

    /**
     * Wrapper for {@link LocalTime#now()}.
     *
     * @return the current time as {@link LocalTime}.
     */
    public LocalTime localTimeNow() {
        return LocalTime.now();
    }

    /**
     * Wrapper for {@link Instant#now()}.
     *
     * @return current time as {@link Instant}.
     */
    public Instant instantNow() {
        return Instant.now();
    }

    /**
     * Wrapper for {@link System#currentTimeMillis()}.
     *
     * @return the difference, measured in milliseconds, between the current time and midnight, January 1, 1970 UTC.
     */
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * Wrapper for {@link System#nanoTime()}.
     *
     * @return the current value of the running Java Virtual Machine's high-resolution time source, in nanoseconds.
     */
    public long nanoTime() {
        return System.nanoTime();
    }
}
