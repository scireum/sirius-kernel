/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import sirius.kernel.Sirius;
import sirius.kernel.di.std.Register;

import java.time.Clock;
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

    private Clock clock = Clock.systemDefaultZone();
    private Clock utcClock = Clock.systemDefaultZone();

    /**
     * Wrapper for {@link LocalDateTime#now()}.
     *
     * @return the current time as {@link LocalDateTime}.
     */
    public LocalDateTime localDateTimeNow() {
        return LocalDateTime.now(clock);
    }

    /**
     * Wrapper for {@link LocalDate#now()}.
     *
     * @return the current time as {@link LocalDate}.
     */
    public LocalDate localDateNow() {
        return LocalDate.now(clock);
    }

    /**
     * Wrapper for {@link LocalTime#now()}.
     *
     * @return the current time as {@link LocalTime}.
     */
    public LocalTime localTimeNow() {
        return LocalTime.now(clock);
    }

    /**
     * Wrapper for {@link Instant#now()}.
     *
     * @return current time as {@link Instant}.
     */
    public Instant instantNow() {
        return Instant.now(utcClock);
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

    /**
     * Returns the current clock being used.
     *
     * @return the clock being used for the system default time zone
     */
    public Clock getClock() {
        return clock;
    }

    /**
     * Specifies the clock to use for the system default time zone.
     * <p>
     * NOTE: This must only be used in test environments.
     *
     * @param clock the clock to use
     */
    public void setClock(Clock clock) {
        if (!Sirius.isStartedAsTest()) {
            throw new IllegalStateException("Cannot change the clock in production systems");
        }
        this.clock = clock;
    }

    /**
     * Returns the clock for UTC.
     *
     * @return the clock with UTC as time zone.
     */
    public Clock getUTCClock() {
        return utcClock;
    }

    /**
     * Specifies the clock to use for the UTC time zone.
     * <p>
     * NOTE: This must only be used in test environments.
     *
     * @param clock the clock to use
     */
    public void setUTCClock(Clock clock) {
        if (!Sirius.isStartedAsTest()) {
            throw new IllegalStateException("Cannot change the clock in production systems");
        }

        this.utcClock = clock;
    }
}
