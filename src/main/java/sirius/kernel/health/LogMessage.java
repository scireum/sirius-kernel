/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health;

import sirius.kernel.nls.NLS;

import java.time.Instant;
import java.util.logging.Level;

/**
 * Contains a log message passed from {@link Log} to {@link LogTap}.
 */
public class LogMessage {
    private String message;
    private long timestamp;
    private Level logLevel;
    private Log receiver;
    private StackTraceElement caller;
    private String thread;

    /**
     * Creates a new log message based on the given parameters.
     *
     * @param message  the message to log
     * @param logLevel the level of the message
     * @param receiver the original receiver
     * @param caller   the stack frame where the log message was issued
     * @param thread   the thread in which the message was logged
     */
    public LogMessage(String message, Level logLevel, Log receiver, StackTraceElement caller, String thread) {
        this.caller = caller;
        this.thread = thread;
        this.timestamp = System.currentTimeMillis();
        this.message = message;
        this.logLevel = logLevel;
        this.receiver = receiver;
    }

    /**
     * Contains the logged message.
     *
     * @return the message sent to the logger
     */
    public String getMessage() {
        return message;
    }

    /**
     * Contains the log level used for this message.
     *
     * @return the log level of this message
     */
    public Level getLogLevel() {
        return logLevel;
    }

    /**
     * Returns the logger used to handle this message.
     *
     * @return the logger used to handle this message
     */
    public Log getReceiver() {
        return receiver;
    }

    /**
     * Returns the timestamp when this message was created as string.
     *
     * @return the timestamp when this message was created, formatted as string
     */
    public String getTimestampAsString() {
        return NLS.toUserString(Instant.ofEpochMilli(timestamp));
    }

    /**
     * Returns the timestamp when this message was created.
     *
     * @return the timestamp when this message was created
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the name of the thread which logged the message.
     *
     * @return the name of the thread which logged the message
     */
    public String getThread() {
        return thread;
    }

    /**
     * Returns the stack frame of the method which issued the log message
     *
     * @return the stack frame which issued the log message
     */
    public StackTraceElement getCaller() {
        return caller;
    }
}
