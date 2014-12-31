/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel;

import com.google.common.base.Charsets;
import org.apache.log4j.*;
import org.apache.log4j.spi.LoggerRepository;
import sirius.kernel.commons.Value;
import sirius.kernel.health.Log;

import java.io.File;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * Used to configure the setup of the SIRIUS framework.
 * <p>
 * An instance of this class is passed into {@link Sirius#start(Setup)} to launch the framework. Alternatively
 * {@link #createAndStartEnvironment(ClassLoader)} can be called which configures and starts SIRIUS based on
 * system properties. This is utilized by the sirius-ipl framework which provides an application container
 * (effectively a main class and some control scripts to make it behave like a daemon or Windows Service).
 * <p>
 * To further customize settings, subclass and override the appropriate methods.
 * <p>
 * By default, this class does the following:
 * <ul>
 * <li>Sets the encoding to UTF-8</li>
 * <li>Sets the DNS cache to 10 seconds - by default this would be 'infinite'</li>
 * <li>Redirects all Java Logging to Log4J</li>
 * <li>Creates either a console or file appender for Log4J (PROD = file, DEV = console)</li>
 * </ul>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2014/12
 */
public class Setup {

    /**
     * Determines the mode in which the framework should run. This mainly effects logging and the configuration.
     */
    public static enum Mode {
        DEV, TEST, PROD
    }

    private ClassLoader loader;
    private Mode mode;
    private boolean logToConsole;
    private boolean logToFile;

    /**
     * Creates a new setup for the given mode and class loader.
     *
     * @param mode   the mode to run the framework in
     * @param loader the class loader used for component discovery
     */
    public Setup(Mode mode, ClassLoader loader) {
        this.mode = mode;
        this.loader = loader;
        logToConsole = mode != Mode.PROD || getProperty("console").asBoolean(false);
        logToFile = mode == Mode.PROD;
    }

    /**
     * Creates and starts a new setup based on system properties.
     * <p>
     * Essentially this is <tt>debug</tt> which switches from PROD to DEV and <tt>console</tt> which enables
     * log output to the console even if running in PROD mode.
     *
     * @param loader the class loader to use
     */
    public static void createAndStartEnvironment(ClassLoader loader) {
        Sirius.start(new Setup(getProperty("debug").asBoolean(false) ? Mode.DEV : Mode.PROD, loader));
    }

    /**
     * Initializes the Virtual Machine.
     * <p>
     * This modifies the DNS cache, encoding and logging setup...
     * <p>
     * This method is automatically called by {@link sirius.kernel.Sirius#start(Setup)}
     */
    public void init() {
        setupLogging();
        setupDNSCache();
        setupEncoding();
    }

    /**
     * Returns the loader to use for component discovery.
     *
     * @return the loader to use for component discovery
     */
    public ClassLoader getLoader() {
        return loader;
    }

    /**
     * Returns the mode the framework was started in.
     *
     * @return the mode of the framework
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * Sets UTF-8 as default encoding
     */
    protected void setupEncoding() {
        Sirius.LOG.FINE("Setting " + Charsets.UTF_8.name() + " as default encoding (file.encoding)");
        System.setProperty("file.encoding", Charsets.UTF_8.name());
        Sirius.LOG.FINE("Setting " + Charsets.UTF_8.name() + " as default mime encoding (mail.mime.charset)");
        System.setProperty("mail.mime.charset", Charsets.UTF_8.name());
    }

    /**
     * Sets the DNS cache to a sane value.
     * <p>
     * By default java infinitely caches all DNS entries. Will be changed to 10 seconds...
     */
    protected void setupDNSCache() {
        Sirius.LOG.FINE("Setting DNS-Cache to 10 seconds...");
        java.security.Security.setProperty("networkaddress.cache.ttl", "10");
    }

    /**
     * Reads the given system property.
     *
     * @param property the property to read
     * @return the contents of the property wrapped as {@link Value}
     */
    protected static Value getProperty(String property) {
        return Value.of(System.getProperty(property));
    }


    /**
     * Initializes log4j as logging framework.
     * <p>
     * In development mode, we log everything to the console. In production mode, we use a rolling file appender and
     * log into the logs directory.
     */
    protected void setupLogging() {
        final LoggerRepository repository = Logger.getRootLogger().getLoggerRepository();
        repository.resetConfiguration();
        Logger.getRootLogger().setLevel(Level.INFO);

        if (shouldLogToConsole()) {
            ConsoleAppender console = new ConsoleAppender();
            console.setLayout(new PatternLayout("%d{HH:mm:ss.SSS} %-5p [%X{flow}|%t] %c - %m%n"));
            console.setThreshold(Level.DEBUG);
            console.activateOptions();
            Logger.getRootLogger().addAppender(console);
        }

        if (shouldLogToFile()) {
            File logsDirectory = new File(getLogsDirectory());
            if (!logsDirectory.exists()) {
                logsDirectory.mkdirs();
            }
            DailyRollingFileAppender fa = new DailyRollingFileAppender();
            fa.setName("FileLogger");
            fa.setFile(getLogFileName());
            fa.setLayout(new PatternLayout("%d %-5p [%X{flow}|%t] %c - %m%n"));
            fa.setThreshold(Level.DEBUG);
            fa.setAppend(true);
            fa.activateOptions();
            Logger.getRootLogger().addAppender(fa);
        }

        redirectJavaLoggerToLog4j();
    }

    /**
     * Returns the name of the log directory.
     *
     * @return the name of the log directory
     */
    protected String getLogsDirectory() {
        return "logs";
    }

    /**
     * Computes the effective name for the log file.
     *
     * @return computes the log file which is getLogsDirectory() / getLogFileName()
     */
    protected String getLogFilePath() {
        return getLogsDirectory() + File.separator + getLogFileName();
    }

    /**
     * Returns the name of the log file.
     *
     * @return the name of the log file
     */
    protected String getLogFileName() {
        return "application.log";
    }

    /**
     * Determines if a console appender should be installed
     *
     * @return <tt>true</tt> if the framework should log to the console
     */
    protected boolean shouldLogToConsole() {
        return logToConsole;
    }

    /**
     * Determines if a file appender should be installed
     *
     * @return <tt>true</tt> if the framework should log into a file
     */
    protected boolean shouldLogToFile() {
        return logToFile;
    }

    /**
     * Redirects all java.logging output to Log4j
     */
    protected void redirectJavaLoggerToLog4j() {
        final LoggerRepository repository = Logger.getRootLogger().getLoggerRepository();
        java.util.logging.Logger rootLogger = java.util.logging.LogManager.getLogManager().getLogger("");
        // remove old handlers
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }
        // add our own
        Handler handler = new Handler() {

            private Formatter formatter = new SimpleFormatter();

            @Override
            public void publish(LogRecord record) {
                repository.getLogger(record.getLoggerName())
                          .log(Log.convertJuliLevel(record.getLevel()),
                               formatter.formatMessage(record),
                               record.getThrown());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }
        };
        handler.setLevel(java.util.logging.Level.ALL);
        rootLogger.addHandler(handler);
        rootLogger.setLevel(java.util.logging.Level.INFO);
    }

}
