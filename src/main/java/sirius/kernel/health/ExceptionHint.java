/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Represents a processing hint which can be attached to a {@link HandledException}.
 * <p>
 * In some scenarios we want to attach additional infos to an exception being generated in order to retrieve them
 * later. One case would be HTTP status codes in sirius-web. In order to ensure that the proper hint names are used
 * and also to make them easily discoverable, we use this class to mark legal hint names.
 */
public class ExceptionHint {

    private final String name;

    /**
     * Creates a new processing hint name.
     * <p>
     * Note that these are most probably kept around as constant to be used and referenced from a central place.
     *
     * @param name the name of the hint
     */
    public ExceptionHint(@Nonnull String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExceptionHint that = (ExceptionHint) o;
        return name.equals(that.name);
    }

    @Override
    public String toString() {
        return name;
    }
}
