/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import sirius.kernel.async.Future;
import sirius.kernel.async.Operation;
import sirius.kernel.async.Tasks;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Value;
import sirius.kernel.commons.Watch;
import sirius.kernel.di.Injector;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.PriorityParts;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;
import sirius.kernel.nls.NLS;
import sirius.kernel.settings.ExtendedSettings;

import javax.annotation.Nullable;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.net.URLConnection;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Loads and initializes the framework.
 * <p>
 * This can be considered the <tt>stage2</tt> when booting the framework, as it is responsible to discover and
 * initialize all components. Call {@link #start(Setup)} to initialize the framework.
 * <p>
 * To make a jar or other classpath-root visible to SIRIUS an empty file called "component.marker" must be placed in
 * its root directory.
 */
public class Sirius {

    /**
     * Contains the name of the system property which is used to select which scenario to execute.
     */
    public static final String SIRIUS_TEST_SCENARIO_PROPERTY = "SIRIUS_TEST_SCENARIO";

    private static final String CONFIG_KEY_CUSTOMIZATIONS = "sirius.customizations";
    private static final String SEPARATOR_LINE = "---------------------------------------------------------";
    private static Setup setup;
    private static Config config;
    private static ExtendedSettings settings;
    private static Map<String, Boolean> frameworks = new HashMap<>();
    private static List<String> customizations = new ArrayList<>();
    private static Classpath classpath;
    private static volatile boolean started = false;
    private static volatile boolean initialized = false;
    private static final long START_TIMESTAMP = System.currentTimeMillis();

    protected static final Log LOG = Log.get("sirius");

    private static final String CONFIG_INSTANCE = "instance";

    @PriorityParts(Startable.class)
    private static List<Startable> lifecycleStartParticipants;

    @PriorityParts(Stoppable.class)
    private static List<Stoppable> lifecycleStopParticipants;

    @PriorityParts(Killable.class)
    private static List<Killable> lifecycleKillParticipants;

    @Part
    private static Tasks tasks;

    private Sirius() {
    }

    /**
     * Determines if the framework is running in development mode.
     *
     * @return {@code true} if the framework runs in development mode, {@code false} otherwise.
     */
    public static boolean isDev() {
        return setup != null && setup.getMode() == Setup.Mode.DEVELOP;
    }

    /**
     * Determines if the framework is running in test mode.
     *
     * @return {@code true} if the framework runs in test mode, {@code false} otherwise.
     */
    public static boolean isTest() {
        return setup != null && setup.getMode() == Setup.Mode.TEST;
    }

    /**
     * Determines if the framework is running in staging mode.
     *
     * @return {@code true} if the framework runs in staging mode, {@code false} otherwise
     */
    public static boolean isStaging() {
        return setup != null && setup.getMode() == Setup.Mode.STAGING;
    }

    /**
     * Determines if the framework was started as test run (JUNIT or the like).
     *
     * @return <tt>true</tt> if the framework was started as test, <tt>false</tt> otherwise
     */
    public static boolean isStartedAsTest() {
        return setup != null && setup.getMode() == Setup.Mode.TEST;
    }

    /**
     * Determines if the framework is running in production mode.
     *
     * @return {@code true} if the framework runs in production mode, {@code false} otherwise.
     */
    public static boolean isProd() {
        return setup != null && setup.getMode() == Setup.Mode.PROD;
    }

    /**
     * Determines if the framework is up and running.
     * <p>
     * This flag will be set to <tt>true</tt> once the framework is being setup and will be immediately
     * set to <tt>false</tt> one the framework starts to shut down.
     *
     * @return <tt>true</tt> once the framework is setup and running and not shutting down yet.
     * @see Tasks#isRunning() Provides a similar flag with slightly different semantics
     */
    public static boolean isRunning() {
        return started;
    }

    /*
     * Once the configuration is loaded, this method applies the log level to all log4j and java.util.logging
     * loggers
     */
    private static void setupLogLevels() {
        if (config.hasPath("log")) {
            LOG.WARN("Found 'log' in the system configuration - use 'logging' to configure loggers!");
        }
        if (config.hasPath("logger")) {
            LOG.WARN("Found 'logger' in the system configuration - use 'logging' to configure loggers!");
        }
        if (config.hasPath("logs")) {
            LOG.WARN("Found 'logs' in the system configuration - use 'logging' to configure loggers!");
        }

        if (!config.hasPath("logging")) {
            LOG.INFO("No 'logger' section in the system config - using defaults...");
            return;
        }

        LOG.INFO("Initializing the log system:");
        Config logging = config.getConfig("logging");
        for (Map.Entry<String, com.typesafe.config.ConfigValue> entry : logging.entrySet()) {
            LOG.INFO("* Setting %s to: %s", entry.getKey(), logging.getString(entry.getKey()));
            Log.setLevel(entry.getKey(), Log.parseLevel(logging.getString(entry.getKey())));
        }
    }

    /*
     * Scans the system config (sirius.frameworks) and determines which frameworks are enabled. This will affect
     * which classes are loaded into the component model.
     */
    private static void setupFrameworks() {
        Config frameworkConfig = config.getConfig("sirius.frameworks");
        Map<String, Boolean> frameworkStatus = new HashMap<>();
        int total = 0;
        int numEnabled = 0;
        LOG.DEBUG_INFO("Scanning framework status (sirius.frameworks):");
        for (Map.Entry<String, com.typesafe.config.ConfigValue> entry : frameworkConfig.entrySet()) {
            String framework = entry.getKey();
            try {
                boolean enabled = Value.of(entry.getValue().unwrapped()).asBoolean(false);
                frameworkStatus.put(framework, enabled);
                total++;
                numEnabled += enabled ? 1 : 0;
                LOG.DEBUG_INFO(Strings.apply("  * %s: %b", framework, enabled));
            } catch (Exception e) {
                Exceptions.ignore(e);
                LOG.WARN("Cannot convert status '%s' of framework '%s' to a boolean! Framework will be disabled.",
                         entry.getValue().render(),
                         framework);
                frameworkStatus.put(framework, false);
            }
        }
        LOG.INFO("Enabled %d of %d frameworks...", numEnabled, total);
        // Although customizations are loaded in setupConfiguration, we output the status here,
        // as this seems more intiutive for the customer (the poor guy reading the logs...)
        LOG.INFO("Active Customizations: %s", customizations);

        frameworks = frameworkStatus;
    }

    /*
     * Starts all framework components
     */
    private static void startComponents() {
        if (started) {
            stop();
        }
        boolean startFailed = false;
        for (final Startable lifecycle : lifecycleStartParticipants) {
            Future future = tasks.defaultExecutor().fork(() -> startLifecycle(lifecycle));
            if (!future.await(Duration.ofMinutes(1))) {
                LOG.WARN("Lifecycle '%s' did not start within one minute....", lifecycle.getClass().getName());
                startFailed = true;
            }
        }

        if (startFailed) {
            outputActiveOperations();
        }
        started = true;
    }

    private static void startLifecycle(Startable lifecycle) {
        LOG.INFO("Starting: %s", lifecycle.getClass().getName());
        try {
            lifecycle.started();
        } catch (Exception e) {
            Exceptions.handle()
                      .error(e)
                      .to(LOG)
                      .withSystemErrorMessage("Startup of: %s failed!", lifecycle.getClass().getName())
                      .handle();
        }
    }

    /**
     * Discovers all components in the class path and initializes the {@link Injector}
     */
    private static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        setupLocalConfig();

        setupClasspath();

        setupApplicationAndSystemConfig();

        LOG.INFO(SEPARATOR_LINE);

        // Setup log-system based on configuration
        setupLogLevels();

        // Output enabled frameworks...
        setupFrameworks();

        // Setup native language support
        NLS.init(classpath);

        // Initialize dependency injection...
        Injector.init(classpath);

        startComponents();

        // Start resource monitoring...
        NLS.startMonitoring(classpath);
    }

    /**
     * Loads the local configuration.
     * <p>
     * These are <tt>settings.conf</tt>, <tt>develop.conf</tt>, <tt>test.conf</tt>
     * and finally, <tt>instance.conf</tt>.
     */
    private static void setupLocalConfig() {
        LOG.INFO("Loading local config...");
        LOG.INFO(SEPARATOR_LINE);

        config = ConfigFactory.empty();

        Config instanceConfig = null;
        if (isStartedAsTest()) {
            config = setup.applyTestConfig(config);
            config = setup.applyTestScenarioConfig(System.getProperty(SIRIUS_TEST_SCENARIO_PROPERTY), config);
        } else {
            // instance.conf and develop.conf are not used to tests to permit uniform behaviour on local
            // machines and build servers...
            config = setup.applyConfig(config, setup.getMode().toString().toLowerCase());
            instanceConfig = setup.applyConfig(config, CONFIG_INSTANCE);
        }

        // Setup customer customizations...
        if (instanceConfig != null && instanceConfig.hasPath(CONFIG_KEY_CUSTOMIZATIONS)) {
            customizations = instanceConfig.getStringList(CONFIG_KEY_CUSTOMIZATIONS);
        } else if (config.hasPath(CONFIG_KEY_CUSTOMIZATIONS)) {
            customizations = config.getStringList(CONFIG_KEY_CUSTOMIZATIONS);
        }

        // Load settings.conf for customizations...
        for (String conf : customizations) {
            if (Sirius.class.getResource("/customizations/" + conf + "/settings.conf") != null) {
                LOG.INFO("loading settings.conf for customization '" + conf + "'");
                String configName = "customizations/" + conf + "/settings.conf";
                try {
                    config = ConfigFactory.parseResources(setup.getLoader(), configName).withFallback(config);
                } catch (Exception e) {
                    handleConfigError(configName, e);
                }
            } else {
                LOG.INFO("customization '" + conf + "' has no settings.conf...");
            }
        }

        // Apply instance config at last for override all other configs...
        if (instanceConfig != null) {
            config = instanceConfig.withFallback(config);
        }

        // Apply environment settings last, as these are often used in docker(-compose) setups
        config = setup.applyEnvironment(config);

        LOG.INFO(SEPARATOR_LINE);
    }

    /**
     * Loads the application config files.
     * <p>
     * These are <tt>component-XXX.conf</tt>, <tt>component-test-XXX.conf</tt>, <tt>application-XXX.conf</tt>
     * and finally, <tt>application.conf</tt>.
     */
    private static void setupApplicationAndSystemConfig() {
        LOG.INFO("Loading system config...");
        LOG.INFO(SEPARATOR_LINE);

        config = config.withFallback(setup.loadApplicationConfig());

        if (isStartedAsTest()) {
            // Load test configurations (will override component configs)
            classpath.find(Pattern.compile("component-test-([^.]*?)\\.conf")).forEach(value -> {
                try {
                    LOG.INFO("Loading test config: %s", value.group());
                    config = config.withFallback(ConfigFactory.parseResources(setup.getLoader(), value.group()));
                } catch (Exception e) {
                    handleConfigError(value.group(), e);
                }
            });
        }

        // Load component configurations
        classpath.find(Pattern.compile("component-([^\\-]*?)([^.]*?)\\.conf")).forEach(value -> {
            if (!"test".equals(value.group(1))) {
                try {
                    LOG.INFO("Loading config: %s", value.group());
                    config = config.withFallback(ConfigFactory.parseResources(setup.getLoader(), value.group()));
                } catch (Exception e) {
                    handleConfigError(value.group(), e);
                }
            }
        });

        config = config.resolve();

        LOG.INFO(SEPARATOR_LINE);
    }

    private static void setupClasspath() {
        if (Sirius.isDev()) {
            // in a local dev environment we disable caching jar connections to hotswap libs especially templates
            URLConnection.setDefaultUseCaches("jar", false);
        }
        classpath = new Classpath(setup.getLoader(), "component.marker", customizations);

        classpath.getComponentRoots().forEach(url -> LOG.INFO("Classpath: %s", url));
    }

    private static void handleConfigError(String file, Exception e) {
        Exceptions.ignore(e);
        Sirius.LOG.WARN("Cannot load %s: %s", file, e.getMessage());
    }

    /**
     * Provides access to the classpath used to load the framework.
     *
     * @return the classpath used to load the framework
     */
    public static Classpath getClasspath() {
        return classpath;
    }

    /**
     * Stops the framework.
     */
    public static void stop() {
        if (!started) {
            return;
        }
        started = false;

        LOG.INFO("Stopping Sirius");
        LOG.INFO(SEPARATOR_LINE);
        outputActiveOperations();
        stopLifecycleParticipants();
        outputActiveOperations();
        waitForLifecyclePaticipants();
        outputThreadState();
        initialized = false;
        settings = null;
    }

    private static void outputThreadState() {
        LOG.INFO("System halted! - Thread State");
        LOG.INFO(SEPARATOR_LINE);
        LOG.INFO("%-15s %10s %53s", "STATE", "ID", "NAME");
        for (ThreadInfo info : ManagementFactory.getThreadMXBean().dumpAllThreads(false, false)) {
            LOG.INFO("%-15s %10s %53s", info.getThreadState().name(), info.getThreadId(), info.getThreadName());
        }
        LOG.INFO(SEPARATOR_LINE);
    }

    private static void waitForLifecyclePaticipants() {
        LOG.INFO("Awaiting system halt...");
        LOG.INFO(SEPARATOR_LINE);
        for (int i = lifecycleKillParticipants.size() - 1; i >= 0; i--) {
            Killable killable = lifecycleKillParticipants.get(i);
            try {
                Watch w = Watch.start();
                killable.awaitTermination();
                LOG.INFO("Terminated: %s (Took: %s)", killable.getClass().getName(), w.duration());
            } catch (Exception e) {
                Exceptions.handle()
                          .error(e)
                          .to(LOG)
                          .withSystemErrorMessage("Termination of: %s failed!", killable.getClass().getName())
                          .handle();
            }
        }
    }

    private static void stopLifecycleParticipants() {
        LOG.INFO("Stopping lifecycles...");
        LOG.INFO(SEPARATOR_LINE);
        for (int i = lifecycleStopParticipants.size() - 1; i >= 0; i--) {
            Stoppable stoppable = lifecycleStopParticipants.get(i);
            Future future = tasks.defaultExecutor().fork(() -> stopLifecycle(stoppable));
            if (!future.await(Duration.ofSeconds(10))) {
                LOG.WARN("Lifecycle '%s' did not stop within 10 seconds....", stoppable.getClass().getName());
            }
        }
        LOG.INFO(SEPARATOR_LINE);
    }

    private static void stopLifecycle(Stoppable lifecycle) {
        LOG.INFO("Stopping: %s", lifecycle.getClass().getName());
        try {
            lifecycle.stopped();
        } catch (Exception e) {
            Exceptions.handle()
                      .error(e)
                      .to(LOG)
                      .withSystemErrorMessage("Stop of: %s failed!", lifecycle.getClass().getName())
                      .handle();
        }
    }

    private static void outputActiveOperations() {
        if (!Operation.getActiveOperations().isEmpty()) {
            LOG.INFO("Active Operations");
            LOG.INFO(SEPARATOR_LINE);
            for (Operation op : Operation.getActiveOperations()) {
                LOG.INFO(op.toString());
            }
            LOG.INFO(SEPARATOR_LINE);
        }
    }

    /**
     * Initializes the framework.
     * <p>
     * This is called by <tt>IPL.main</tt> once the class loader is fully populated.
     *
     * @param setup the setup class used to configure the framework
     */
    public static void start(Setup setup) {
        Watch w = Watch.start();
        Sirius.setup = setup;
        setup.init();
        LOG.INFO(SEPARATOR_LINE);
        LOG.INFO("System is STARTING...");
        LOG.INFO(SEPARATOR_LINE);
        init();
        LOG.INFO(SEPARATOR_LINE);
        LOG.INFO("System is UP and RUNNING - %s", w.duration());
        LOG.INFO(SEPARATOR_LINE);

        Runtime.getRuntime().addShutdownHook(new Thread(Sirius::stop));
    }

    /**
     * Determines if the framework with the given name is enabled.
     * <p>
     * Frameworks can be enabled or disabled using the config path <tt>sirius.framework.[name]</tt>. This is
     * intensively used by the app part, as it provides a lot of basic frameworks which can be turned off or
     * on as required.
     *
     * @param framework the framework to check
     * @return <tt>true</tt> if the framework is enabled, <tt>false</tt> otherwise
     */
    public static boolean isFrameworkEnabled(String framework) {
        if (Strings.isEmpty(framework)) {
            return true;
        }
        if (Sirius.isDev() && !frameworks.containsKey(framework)) {
            LOG.WARN("Status of unknown framework '%s' requested. Will report as disabled framework.", framework);
        }
        return Boolean.TRUE.equals(frameworks.get(framework));
    }

    /**
     * Returns a list of all active customer configurations.
     * <p>
     * A customer configuration can be used to override basic functionality with more specialized classes or resources
     * which were adapted based on customer needs.
     * <p>
     * As often groups of customers share the same requirements, not only a single configuration can be activated
     * but a list. Within this list each configuration may override classes and resources of all former
     * configurations. Therefore the last configuration will always "win".
     * <p>
     * Note that classes must be placed in the package: <b>configuration.[name]</b> (with arbitrary sub packages).
     * Also resources must be placed in: <b>configuration/[name]/resource-path</b>.
     *
     * @return a list of all active configurations
     */
    public static List<String> getActiveConfigurations() {
        return Collections.unmodifiableList(customizations);
    }

    /**
     * Determines if the given config is active (or null which is considered active)
     *
     * @param configName the name of the config to check. <tt>null</tt> will be considered as active
     * @return <tt>true</tt> if the named customization is active, <tt>false</tt> otherwise
     */
    public static boolean isActiveCustomization(@Nullable String configName) {
        return configName == null || Sirius.getActiveConfigurations().contains(configName);
    }

    /**
     * Determines if the given resource is part of a customization.
     *
     * @param resource the resource path to check
     * @return <tt>true</tt> if the given resource is part of a customization, <tt>false</tt> otherwise
     */
    public static boolean isCustomizationResource(@Nullable String resource) {
        return resource != null && resource.startsWith("customizations");
    }

    /**
     * Extracts the customization name from a resource.
     * <p>
     * Valid names are paths like "customizations/[name]/..." or classes like "customizations.[name]...".
     *
     * @param resource the name of the resource
     * @return the name of the customizations or <tt>null</tt> if no config name is contained
     */
    @Nullable
    public static String getCustomizationName(@Nullable String resource) {
        if (resource == null) {
            return null;
        }
        if (resource.startsWith("customizations/")) {
            return Strings.split(resource.substring(15), "/").getFirst();
        } else if (resource.startsWith("customizations.")) {
            return Strings.split(resource.substring(15), ".").getFirst();
        } else {
            return null;
        }
    }

    /**
     * Compares the two given customizations according to the order given in the system config.
     *
     * @param configA the name of the first customization
     * @param configB the name of the second customization
     * @return an int value which can be used to compare the order of the two given configs.
     */
    public static int compareCustomizations(@Nullable String configA, @Nullable String configB) {
        if (configA == null) {
            if (configB == null) {
                return 0;
            }
            return 1;
        }
        if (configB == null) {
            return -1;
        }
        return customizations.indexOf(configA) - customizations.indexOf(configB);
    }

    /**
     * Returns the system config based on the current instance.conf (file system), application.conf (classpath) and
     * all component-XXX.conf wrapped as <tt>Settings</tt>
     *
     * @return the initialized settings object or <tt>null</tt> if the framework is not setup yet.
     */
    public static ExtendedSettings getSettings() {
        if (settings == null && config != null) {
            settings = new ExtendedSettings(config, true);
        }
        return settings;
    }

    /**
     * Provides access to the setup instance which was used to configure Sirius.
     *
     * @return the setup instance used to configure Sirius.+
     */
    public static Setup getSetup() {
        return setup;
    }

    /**
     * Returns the up time of the system in milliseconds.
     *
     * @return the number of milliseconds the system is running
     */
    public static long getUptimeInMilliseconds() {
        return System.currentTimeMillis() - START_TIMESTAMP;
    }
}
