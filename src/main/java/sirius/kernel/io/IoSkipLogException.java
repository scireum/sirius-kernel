/*
 * Made with all the love in the world
 * by scireum in Stuttgart, Germany
 *
 * Copyright by scireum GmbH
 * https://www.scireum.de - info@scireum.de
 */

package sirius.kernel.io;

import sirius.kernel.xml.Outcall;

import java.io.IOException;
import java.io.Serial;

/**
 * This class gets used as an {@linkplain IOException#getCause() IOException's cause} to mark that this exception
 * should not be logged.
 * <p>
 * Possible, to be skipped log messages are those, that contain no information and only spoil the log file, like
 * error from requests that not get executed due to the blacklisting feature of {@linkplain Outcall outcall}.
 */
public class IoSkipLogException extends Exception {

    @Serial
    private static final long serialVersionUID = 4787224866678714833L;
}
