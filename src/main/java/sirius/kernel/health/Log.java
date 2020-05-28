/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health;

import sirius.kernel.Sirius;
import sirius.kernel.commons.Explain;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.PartCollection;
import sirius.kernel.di.std.Parts;
import sirius.kernel.nls.NLS;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * The logging facade used by the system.
 * <p>
 * <b>Note:</b> Instead of "just" logging exceptions, handle them with {@link sirius.kernel.health.Exceptions#handle()}
 * to generate sophisticated error messages and to permit other parts of the framework to intercept error
 * handling.
 * <p>
 * In contrast to other approaches, it is not recommended to create a logger per class, but rather one per
 * framework or sub system. It should have a concise name, all lowercase without any dots. The log level of each
 * logger is read from the configuration using {@code logging.[NAME]}. It may be set to one of:
 * <ul>
 * <li>DEBUG</li>
 * <li>INFO</li>
 * <li>WARN</li>
 * <li>ERROR</li>
 * </ul>
 * <p>
 * Internally uses log4j to perform all logging operations. Still it is recommended to only log through this facade
 * and not to rely on any log4j specific behaviour.
 */
@SuppressWarnings("squid:S00100")
@Explain("We have these special method names so they stand out from the business logic.")
public class Log {

    private final Logger logger;
    private static final List<Log> all = new CopyOnWriteArrayList<>();

    /**
     * Keeps a hard reference to each {@link Logger} which level has been changed manually.
     * <p>
     * This is required as the wonderful Java logging implementation doesn't retain one. Therefore the logger
     * might get garbage collected <i>and a new logger with the same name</i> is returned.
     */
    private static final Map<String, Logger> loggerHardReferences = new ConcurrentHashMap<>();

    /**
     * Provides a generic logger for application log messages.
     * <p>
     * This should ony be used by application code. Use a specific logger if possible and appropriate.
     * <p>
     * Use {@link #SYSTEM} for generic framework / system specific events.
     */
    public static final Log APPLICATION = Log.get("application");

    /**
     * Provides a generic logger for system log messages.
     * <p>
     * This should only be used by libraries or frameworks. Use a specific logger if possible and appropriate.
     * <p>
     * Use {@link #APPLICATION} for generic application specific events.
     */
    public static final Log SYSTEM = Log.get("system");

    /**
     * Provides a logger used to log events of the background / batch execution system, like {@link sirius.kernel.async.BackgroundLoop background loops}.
     * <p>
     * Use a specific logger if possible and appropriate.
     */
    public static final Log BACKGROUND = Log.get("background");

    /**
     * Used to cut endless loops while feeding taps.
     */
    private static final ThreadLocal<Boolean> frozen = new ThreadLocal<>();

    @Parts(LogTap.class)
    private static PartCollection<LogTap> taps;

    /*
     * Use get(String) to create a new instance
     */
    private Log(Logger logger) {
        super();
        this.logger = logger;
    }

    /**
     * Generates a new logger with the given name
     * <p>
     * The given name should be short and simple. It is not recommended to create a logger per class but one for
     * each framework or subsystem.
     *
     * @param name the name of the logger. This should be a simple name, completely lowercase, without any dots
     * @return a new logger logging with the given name.
     */
    @SuppressWarnings("squid:S2250")
    @Explain("Loggers are only created once, so there is no performance hotspot")
    public static synchronized Log get(String name) {
        Log result = new Log(Logger.getLogger(name));
        all.add(result);
        if (!name.matches("[a-z0-9\\-]+")) {
            result.WARN("Invalid logger name: %s. Only numbers, lowercase letters and - are allowed!%n", name);
        }
        return result;
    }

    /**
     * Returns a list of all known loggers.
     *
     * @return a list of all known loggers
     */
    public static List<Log> getAllLoggers() {
        return Collections.unmodifiableList(all);
    }

    /**
     * Helper method the change the log-level for a given logger.
     * <p>
     * By default the system configuration is used to set the leg level (logging.[name]=LEVEL). Also the "logger"
     * command in the console can be used to change the log level at runtime.
     *
     * @param logger the name of the logger to change
     * @param level  the desired log level
     */
    public static void setLevel(String logger, Level level) {
        loggerHardReferences.computeIfAbsent(logger, Logger::getLogger).setLevel(level);
    }

    private void log(Level level, Object msg) {
        StackTraceElement caller = new Throwable().getStackTrace()[2];

        LogRecord logRecord = createLogRecord(level, msg, caller);
        if (msg instanceof Throwable) {
            logRecord.setThrown((Throwable) msg);
        }

        logger.log(logRecord);
        tap(msg, caller, level);
    }

    private LogRecord createLogRecord(Level level, Object msg, StackTraceElement caller) {
        LogRecord logRecord = new LogRecord(level, msg.toString());
        logRecord.setLoggerName(logger.getName());
        logRecord.setSourceClassName(caller.getFileName() + ":" + caller.getLineNumber());
        return logRecord;
    }

    private void tap(Object msg, StackTraceElement caller, Level level) {
        if (Boolean.TRUE.equals(frozen.get())) {
            return;
        }
        if (taps == null) {
            return;
        }

        try {
            frozen.set(Boolean.TRUE);
            for (LogTap tap : taps) {
                try {
                    tap.handleLogMessage(new LogMessage(NLS.toUserString(msg),
                                                        level,
                                                        this,
                                                        caller,
                                                        Thread.currentThread().getName()));
                } catch (Exception e) {
                    // Ignored - if we can't log s.th. let's just give up...
                }
            }
        } finally {
            frozen.set(Boolean.FALSE);
        }
    }

    /**
     * Logs the given message at INFO level
     * <p>
     * The given object is converted to a string if necessary. The INFO level should be used for informative
     * messages to the system operator which occur at a low rate
     *
     * @param msg the message to be logged
     */
    public void INFO(Object msg) {
        if (msg == null || !logger.isLoggable(Level.INFO)) {
            return;
        }

        log(Level.INFO, msg);
    }

    /**
     * Used to log the given message <tt>msg</tt> at <b>INFO</b> level if debug mode is enabled
     * ({@link sirius.kernel.Sirius#isDev()}). Otherwise the message will be logged as <b>FINE</b>.
     *
     * @param msg the message to log
     */
    public void DEBUG_INFO(Object msg) {
        if (Sirius.isDev()) {
            INFO(msg);
        } else {
            FINE(msg);
        }
    }

    /**
     * Formats the given message at the INFO level using the supplied parameters.
     * <p>
     * The INFO level should be used for informative messages to the system operator which occur at a low rate
     *
     * @param msg    the message containing placeholders as understood by {@link Strings#apply(String, Object...)}
     * @param params the parameters used to format the resulting log message
     */
    public void INFO(String msg, Object... params) {
        if (logger.isLoggable(Level.INFO)) {
            log(Level.INFO, Strings.apply(msg, params));
        }
    }

    /**
     * Logs the given message at the FINE level
     * <p>
     * The given object is converted to a string if necessary. The FINE level can be used for in depth debug or trace
     * messages used when developing a system. Sill the rate should be kept bearable to enable this level in
     * production systems to narrow down errors.
     *
     * @param msg the message to be logged
     */
    public void FINE(Object msg) {
        if (msg == null || !logger.isLoggable(Level.FINE)) {
            return;
        }

        log(Level.FINE, msg);
    }

    /**
     * Formats the given message at the FINE level using the supplied parameters.
     * <p>
     * The FINE level can be used for in depth debug or trace messages used when developing a system.
     * Sill the rate should be kept bearable to enable this level in production systems to narrow down errors.
     *
     * @param msg    the message containing placeholders as understood by {@link Strings#apply(String, Object...)}
     * @param params the parameters used to format the resulting log message
     */
    public void FINE(String msg, Object... params) {
        if (logger.isLoggable(Level.FINE)) {
            log(Level.FINE, Strings.apply(msg, params));
        }
    }

    /**
     * Logs the given message at the WARN level
     * <p>
     * The given object is converted to a string if necessary. The WARN level can be used to signal unexpected
     * situations which do not (yet) result in an error or problem.
     *
     * @param msg the message to be logged
     */
    public void WARN(Object msg) {
        if (msg == null || !logger.isLoggable(Level.WARNING)) {
            return;
        }

        log(Level.WARNING, msg);
    }

    /**
     * Formats the given message at the WARN level using the supplied parameters.
     * <p>
     * The WARN level can be used to signal unexpected situations which do not (yet) result in an error or problem.
     *
     * @param msg    the message containing placeholders as understood by {@link Strings#apply(String, Object...)}
     * @param params the parameters used to format the resulting log message
     */
    public void WARN(String msg, Object... params) {
        if (logger.isLoggable(Level.WARNING)) {
            log(Level.WARNING, Strings.apply(msg, params));
        }
    }

    /**
     * Logs the given message at the SEVERE or ERROR level
     * <p>
     * The given object is converted to a string if necessary. The ERROR level can be used to signal problems or error
     * which occurred in the system. It is recommended to handle exceptions using {@link Exceptions} - which will
     * eventually also call this method, but provides sophisticated error handling.
     *
     * @param msg the message to be logged
     */
    public void SEVERE(Object msg) {
        if (msg == null || !logger.isLoggable(Level.SEVERE)) {
            return;
        }

        log(Level.SEVERE, msg);
    }

    /**
     * Determines if FINE message will be logged.
     * <p>
     * This can be used to decide whether "expensive" log messages should be constructed at all. Using
     * {@link #FINE(String, Object...)} doesn't require this check since the message is only formatted if it will be
     * logged. However, if the computation of one of the parameters is complex, one might sill want to surround the
     * log message by an appropriate if statement calling this method.
     *
     * @return <tt>true</tt> if this logger logs FINE message, <tt>false</tt> otherwise
     */
    public boolean isFINE() {
        return logger.isLoggable(Level.FINE);
    }

    /**
     * Returns the name of this logger
     *
     * @return the name supplied by {@link #get(String)}.
     */
    public String getName() {
        return logger.getName();
    }

    /**
     * Returns the effective log level of this logger.
     *
     * @return the effective log level
     */
    public Level getLevel() {
        return logger.getLevel();
    }
}
