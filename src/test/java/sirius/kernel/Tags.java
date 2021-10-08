/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel;

/**
 * Provides common values to be used at org.junit.jupiter.api.Tag annotations.
 */
public class Tags {

    /**
     * Tag value to express that nightly-only test execution is wished.
     * <p>
     * See also: .drone.yml with exemplary maven call: -Dtest.excluded.groups=nightly
     */
    public static final String NIGHTLY = "nightly";
}
