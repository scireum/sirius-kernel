/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel;

import com.google.common.base.Charsets;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggerRepository;
import sirius.kernel.commons.Value;
import sirius.kernel.health.Log;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Arrays;
import java.util.function.Predicate;
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
 */
public class Setup {

    /**
     * Determines the mode in which the framework should run. This mainly effects logging and the configuration.
     */
    public enum Mode {
        DEV, TEST, PROD
    }

    protected ClassLoader loader;
    protected Mode mode;
    protected boolean logToConsole;
    protected boolean logToFile;
    protected Level defaultLevel = Level.INFO;
    protected String consoleLogFormat = "%d{HH:mm:ss.SSS} %-5p [%X{flow}|%t] %c - %m%n";
    protected String fileLogFormat = "%d %-5p [%X{flow}|%t] %c - %m%n";

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
     * Overwrites the settings for the console appender.
     *
     * @param flag determines if logging to the console is enabled or not.
     * @return the setup itself for fluent method calls
     */
    public Setup withLogToConsole(boolean flag) {
        this.logToConsole = flag;
        return this;
    }

    /**
     * Overwrites the settings for the file appender.
     *
     * @param flag determines if logging to the log file is enabled or not.
     * @return the setup itself for fluent method calls
     */
    public Setup withLogToFile(boolean flag) {
        this.logToFile = flag;
        return this;
    }

    /**
     * Used to set the default log level used by the root logger.
     * <p>
     * Note that each logger can be configured by specifying <tt>logging.[NAME]</tt> in the
     * system configuration
     *
     * @param level the level to use
     * @return the setup itself for fluent method calls
     */
    public Setup withDefaultLogLevel(Level level) {
        this.defaultLevel = level;
        return this;
    }

    /**
     * Specifies the pattern used to format log messages in the console.
     * <p>
     * Refer to {@link org.apache.log4j.PatternLayout} for available options.
     *
     * @param format the template string to use
     * @return the setup itself for fluent method calls
     */
    public Setup withConsoleLogFormat(String format) {
        this.consoleLogFormat = format;
        return this;
    }

    /**
     * Specifies the pattern used to format log messages in the log file.
     * <p>
     * Refer to {@link org.apache.log4j.PatternLayout} for available options.
     *
     * @param format the template string to use
     * @return the setup itself for fluent method calls
     */
    public Setup withFileLogFormat(String format) {
        this.fileLogFormat = format;
        return this;
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
        outputJVMInfo();
    }

    /**
     * Outputs the name of the underlying JVM to verify that the correct one was used to start the application
     */
    protected void outputJVMInfo() {
        RuntimeMXBean mx = ManagementFactory.getRuntimeMXBean();
        Sirius.LOG.INFO("%s (%s, %s, %s)", mx.getVmName(), mx.getSpecVersion(), mx.getVmVendor(), mx.getVmVersion());
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
        Logger.getRootLogger().setLevel(defaultLevel);

        if (shouldLogToConsole()) {
            ConsoleAppender console = new ConsoleAppender();
            console.setLayout(new PatternLayout(consoleLogFormat));
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
            fa.setFile(getLogFilePath());
            fa.setLayout(new PatternLayout(fileLogFormat));
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
     * Invoked by Sirius itself on a regular basis to clean old log files.
     *
     * @param retentionInMillis the desired retention time in milli seconds before a file is deleted.
     */
    public void cleanOldLogFiles(long retentionInMillis) {
        if (!shouldLogToFile()) {
            return;
        }

        File logsDir = new File(getLogsDirectory());
        if (!logsDir.exists()) {
            return;
        }
        File[] children = logsDir.listFiles();
        if (children == null) {
            return;
        }

        // The file must start with the log file name, but have an extension (we don't want to delete
        // the main log file).
        Predicate<File> validLogFileName =
                f -> f.getName().startsWith(getLogFileName()) && !f.getName().equals(getLogFileName());

        Predicate<File> isOldEnough = f -> System.currentTimeMillis() - f.lastModified() > retentionInMillis;

        Arrays.asList(children)
              .stream()
              .filter(File::isFile)
              .filter(validLogFileName)
              .filter(isOldEnough)
              .forEach(File::delete);
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
            public void close() {
            }
        };
        handler.setLevel(java.util.logging.Level.ALL);
        rootLogger.addHandler(handler);
        rootLogger.setLevel(java.util.logging.Level.INFO);
    }

    /**
     * Loads the main application configuration which is shipped with the app.
     * <p>
     * By default this loads "application.conf" from the classpath
     *
     * @return the main application config. This will override all component configs but be overridden by developer,
     * test and instance configs
     */
    @Nonnull
    public Config loadApplicationConfig() {
        Config result = ConfigFactory.empty();
        if (Sirius.class.getResource("/application.conf") != null) {
            Sirius.LOG.INFO("using application.conf from classpath...");
            result = ConfigFactory.load(loader, "application.conf").withFallback(result);
        } else {
            Sirius.LOG.INFO("application.conf not present in classpath");
        }

        return result;
    }

    /**
     * Applies the test configuration to the given config object.
     * <p>
     * By default this loads and applies "test.conf" from the classpath.
     *
     * @param config the config to enhance
     * @return the enhanced config
     */
    @Nonnull
    public Config applyTestConfig(@Nonnull Config config) {
        if (Sirius.class.getResource("/test.conf") != null) {
            Sirius.LOG.INFO("using test.conf from classpath...");
            return ConfigFactory.load(loader, "test.conf").withFallback(config);
        } else {
            Sirius.LOG.INFO("test.conf not present in classpath");
            return config;
        }
    }

    /**
     * Applies the developer configuration to the given config object.
     * <p>
     * By default this loads and applies "develop.conf" from the file system.
     *
     * @param config the config to enhance
     * @return the enhanced config
     */
    @Nonnull
    public Config applyDeveloperConfig(@Nonnull Config config) {
        if (new File("develop.conf").exists()) {
            Sirius.LOG.INFO("using develop.conf from filesystem...");
            return ConfigFactory.parseFile(new File("develop.conf")).withFallback(config);
        } else {
            Sirius.LOG.INFO("develop.conf not present in work directory");
            return config;
        }
    }

    /**
     * Loads the instance configuration which configures the app for the machine it is running on.
     * <p>
     * By default this loads "instance.conf" from the file system
     * <p>
     * This will later be applied to the overall system configuration and will override all other settings.
     *
     * @return the instance configuration or <tt>null</tt> if no config was found.
     */
    @Nullable
    public Config loadInstanceConfig() {
        if (new File("instance.conf").exists()) {
            Sirius.LOG.INFO("using instance.conf from filesystem...");
            return ConfigFactory.parseFile(new File("instance.conf"));
        } else {
            Sirius.LOG.INFO("instance.conf not present work in directory");
            return null;
        }
    }
}
