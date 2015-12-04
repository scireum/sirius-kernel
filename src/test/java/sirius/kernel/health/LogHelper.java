/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health;

import org.apache.log4j.Level;
import sirius.kernel.di.std.Part;

import java.util.regex.Pattern;

/**
 * Helps to create tests which expect certain kind of log messages to be created.
 */
public class LogHelper {

    private LogHelper() {}

    @Part
    private static MemoryBasedHealthMonitor monitor;

    /**
     * Clears all log message.
     * <p>
     * It is probably a good idea to clear all previously logged messages prior to a test.
     */
    public static void clearMessages() {
        monitor.messages.clear();
    }

    /**
     * Determines if a message matching the given criteria was logged.
     *
     * @param level   the expected level
     * @param logger  the expected target logger
     * @param pattern the <b>regular expression</b> to <b>find</b> within the message. Use <tt>^foobar$</tt> to match
     *                the whole message.
     * @return <tt>true</tt> if a matching message was found, <tt>false</tt> otherwise
     */
    public static boolean hasMessage(Level level, String logger, String pattern) {
        Pattern regEx = Pattern.compile(pattern);
        for (LogMessage msg : monitor.getMessages()) {
            if (msg.getReceiver().getName().equals(logger)) {
                if (level == msg.getLogLevel()) {
                    if (regEx.matcher(msg.getMessage()).find()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Determines if a message matching the given criteria was logged.
     *
     * @param level   the expected level
     * @param logger  the expected target logger
     * @param pattern the <b>regular expression</b> to <b>find</b> within the message. Use <tt>^foobar$</tt> to match
     *                the whole message.
     * @return <tt>true</tt> if a matching message was found, <tt>false</tt> otherwise
     */
    public static boolean hasMessage(Level level, Log logger, String pattern) {
        return hasMessage(level, logger.getName(), pattern);
    }
}
