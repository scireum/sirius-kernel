/*
 * Made with all the love in the world
 * by scireum in Stuttgart, Germany
 *
 * Copyright by scireum GmbH
 * https://www.scireum.de - info@scireum.de
 */

package sirius.kernel.io;

import sirius.kernel.commons.Outcall;

import java.io.IOException;
import java.io.Serial;

/**
 * This class gets used as a special kind of {@linkplain IOException} to mark that a failed IO operation
 * should not be logged.
 * <p>
 * Possible, to be skipped log messages are those, that contain no information and only spoil the log file, like
 * error from requests that not get executed due to the blacklisting feature of {@linkplain Outcall outcall}.
 */
public class IOExceptionSkipLog extends IOException {

    @Serial
    private static final long serialVersionUID = 4787224866678714833L;

    public IOExceptionSkipLog(String message) {
        super(message);
    }
}
