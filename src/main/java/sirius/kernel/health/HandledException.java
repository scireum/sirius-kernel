/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health;

import sirius.kernel.commons.Value;

import java.io.Serial;
import java.util.Collections;
import java.util.Map;

/**
 * An exception which has already been handled (logged and reacted upon) can be represented by
 * <tt>HandledException</tt>.
 * <p>
 * Instances of this type need no further treatment and guarantee to have a properly translated error message,
 * which van be directly shown to the user.
 * <p>
 * Creating a <tt>HandledException</tt> is done by using one of the static methods of {@link Exceptions}.
 */
public class HandledException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 4460968279048068701L;

    private final transient Map<ExceptionHint, Object> hints;

    /**
     * Creates a new instance with the given message and no exception attached
     *
     * @param message the message to be shown to the user
     * @param hints   any processing hints which have been specified so far
     * @param cause   the exception which actually caused this error
     */
    protected HandledException(String message, Map<ExceptionHint, Object> hints, Throwable cause) {
        super(message, cause);
        this.hints = Collections.unmodifiableMap(hints);
    }

    /**
     * Retrieves the hint which has previously been added.
     *
     * @param hint the name of the hint to fetch
     * @return the hin value (which might be empty)
     * @see Exceptions.ErrorHandler#hint(ExceptionHint, Object)
     */
    public Value getHint(ExceptionHint hint) {
        return Value.of(hints.get(hint));
    }
}
