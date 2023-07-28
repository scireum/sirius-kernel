/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health

import sirius.kernel.di.std.Part
import java.util.logging.Level
import java.util.regex.Pattern

/**
 * Provides a helper for checking whether a certain log message was logged.
 */
object LogHelper {


    @Part
    private lateinit var monitor: MemoryBasedHealthMonitor

    /**
     * Clears all log messages.
     *
     *
     * It is probably a good idea to clear all previously logged messages prior to a test.
     */
    fun clearMessages() {
        monitor.messages.clear()
    }

    /**
     * Determines if a message matching the given criteria was logged.
     *
     * @param level   the expected level
     * @param logger  the expected target logger
     * @param pattern the **regular expression** to **find** within the message. Use <tt>^foobar$</tt> to match
     * the whole message.
     * @return <tt>true</tt> if a matching message was found, <tt>false</tt> otherwise
     */
    private fun hasMessage(level: Level, logger: String, pattern: String): Boolean {
        val regEx = Pattern.compile(pattern)

        return monitor.messages.stream().filter { logger == it.receiver.name }
                .filter { level === it.logLevel }
                .filter { regEx.matcher(it.message).find() }
                .findFirst().isPresent
    }

    /**
     * Determines if a message matching the given criteria was logged.
     *
     * @param level   the expected level
     * @param logger  the expected target logger
     * @param pattern the **regular expression** to **find** within the message. Use <tt>^foobar$</tt> to match
     * the whole message.
     * @return <tt>true</tt> if a matching message was found, <tt>false</tt> otherwise
     */
    fun hasMessage(level: Level, logger: Log, pattern: String): Boolean {
        return hasMessage(level, logger.name, pattern)
    }
}
