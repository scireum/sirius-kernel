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
 * Provides wrapper for static time functions which can be injected using a {@link sirius.kernel.di.std.Part}
 * annotation.
 * <p>
 * Using this wrapper instead of the static methods permits to use mocking in tests.
 */
@Register(classes = TimeProvider.class)
public class TimeProvider {
    public LocalDateTime localDateTimeNow() {
        return LocalDateTime.now();
    }

    public LocalDate localDateNow() {
        return LocalDate.now();
    }

    public LocalTime localTimeNow() {
        return LocalTime.now();
    }

    public Instant instantNow() {
        return Instant.now();
    }

    public long systemCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    public long systemNanoTime() {
        return System.nanoTime();
    }
}
