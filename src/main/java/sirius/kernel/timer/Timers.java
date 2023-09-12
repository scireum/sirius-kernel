/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.timer;

import sirius.kernel.Sirius;
import sirius.kernel.Startable;
import sirius.kernel.Stoppable;
import sirius.kernel.async.Orchestration;
import sirius.kernel.async.Tasks;
import sirius.kernel.commons.Explain;
import sirius.kernel.commons.TimeProvider;
import sirius.kernel.commons.Watch;
import sirius.kernel.di.PartCollection;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Parts;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;
import sirius.kernel.nls.NLS;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Internal service which is responsible for executing timers.
 * <p>
 * Other than for statistical reasons, this class does not need to be called directly. It automatically
 * discovers all parts registered for one of the timer interfaces (<tt>EveryMinute</tt>, <tt>EveryTenMinutes</tt>,
 * <tt>EveryHour</tt>, <tt>EveryDay</tt>) and invokes them appropriately.
 * <p>
 * To access this class, a <tt>Part</tt> annotation can be used on a field of type <tt>TimerService</tt>.
 */
@Register(classes = {Timers.class, Startable.class, Stoppable.class})
public class Timers implements Startable, Stoppable {

    @SuppressWarnings("squid:S1192")
    @Explain("These constants are semantically different.")
    protected static final Log LOG = Log.get("timer");
    private static final String TIMER = "timer";

    /**
     * Contains the config prefix to load settings for daily tasks from.
     */
    public static final String TIMER_DAILY_PREFIX = "timer.daily.";

    private static final int TEN_SECONDS_IN_MILLIS = 10000;

    @Part
    private Tasks tasks;

    @Part
    private TimeProvider timeProvider;

    @Part
    @Nullable
    private Orchestration orchestration;

    @Parts(EveryTenSeconds.class)
    private PartCollection<EveryTenSeconds> everyTenSeconds;
    private long lastTenSecondsExecution = 0;

    @Parts(EveryMinute.class)
    private PartCollection<EveryMinute> everyMinute;
    private long lastOneMinuteExecution = 0;

    @Parts(EveryTenMinutes.class)
    private PartCollection<EveryTenMinutes> everyTenMinutes;
    private long lastTenMinutesExecution = 0;

    @Parts(EveryHour.class)
    private PartCollection<EveryHour> everyHour;
    private long lastHourExecution = 0;

    @Parts(EveryDay.class)
    private PartCollection<EveryDay> everyDay;

    private Timer timer;
    private final ReentrantLock timerLock = new ReentrantLock();

    /*
     * Contains the relative paths of all loaded files
     */
    private final List<WatchedResource> loadedFiles = new CopyOnWriteArrayList<>();

    /*
     * Used to frequently check loaded properties when running in DEVELOP mode.
     */
    private Timer reloadTimer;

    /*
     * Determines the interval which files are checked for update
     */
    private static final int RELOAD_INTERVAL = 1000;

    /**
     * Determines the start and stop order of the timers lifecycle. Exposed as public so that
     * dependent lifecycles can determine their own priority based on this.
     */
    public static final int LIFECYCLE_PRIORITY = 1000;

    @Override
    public int getPriority() {
        return LIFECYCLE_PRIORITY;
    }

    private class InnerTimerTask extends TimerTask {

        @Override
        public void run() {
            try {
                runTenSecondTimers();
                if (TimeUnit.MINUTES.convert(timeProvider.currentTimeMillis() - lastOneMinuteExecution,
                                             TimeUnit.MILLISECONDS) >= 1) {
                    runOneMinuteTimers();
                }
                if (TimeUnit.MINUTES.convert(timeProvider.currentTimeMillis() - lastTenMinutesExecution,
                                             TimeUnit.MILLISECONDS) >= 10) {
                    runTenMinuteTimers();
                }
                if (TimeUnit.MINUTES.convert(timeProvider.currentTimeMillis() - lastHourExecution,
                                             TimeUnit.MILLISECONDS) >= 60) {
                    runOneHourTimers();
                    runEveryDayTimers(timeProvider.localTimeNow().getHour());
                }
            } catch (Exception t) {
                Exceptions.handle(LOG, t);
            }
        }
    }

    /*
     * Used to monitor a resource for changes
     */
    private static class WatchedResource {
        private File file;
        private long lastModified;
        private Runnable callback;
    }

    /**
     * Returns the timestamp of the last execution of the 10-second timer.
     *
     * @return a textual representation of the last execution of the ten seconds timer. Returns "-" if the timer didn't
     * run yet.
     */
    public String getLastTenSecondsExecution() {
        if (lastTenSecondsExecution == 0) {
            return "-";
        }
        return NLS.toUserString(Instant.ofEpochMilli(lastTenSecondsExecution));
    }

    /**
     * Returns the timestamp of the last execution of the one-minute timer.
     *
     * @return a textual representation of the last execution of the one-minute timer. Returns "-" if the timer didn't
     * run yet.
     */
    public String getLastOneMinuteExecution() {
        if (lastOneMinuteExecution == 0) {
            return "-";
        }
        return NLS.toUserString(Instant.ofEpochMilli(lastOneMinuteExecution));
    }

    /**
     * Returns the timestamp of the last execution of the ten minutes timer.
     *
     * @return a textual representation of the last execution of the ten minutes timer. Returns "-" if the timer didn't
     * run yet.
     */
    public String getLastTenMinutesExecution() {
        if (lastTenMinutesExecution == 0) {
            return "-";
        }
        return NLS.toUserString(Instant.ofEpochMilli(lastTenMinutesExecution));
    }

    /**
     * Returns the timestamp of the last execution of the one-hour timer.
     *
     * @return a textual representation of the last execution of the one-hour timer. Returns "-" if the timer didn't
     * run yet.
     */
    public String getLastHourExecution() {
        if (lastHourExecution == 0) {
            return "-";
        }
        return NLS.toUserString(Instant.ofEpochMilli(lastHourExecution));
    }

    @Override
    public void started() {
        if (Sirius.isFrameworkEnabled("kernel.timer")) {
            startTimer();
        }
        if (Sirius.isDev()) {
            startResourceWatcher();
        }
    }

    private void startResourceWatcher() {
        if (reloadTimer == null) {
            reloadTimer = new Timer(true);
            reloadTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    watchLoadedResources();
                }
            }, RELOAD_INTERVAL, RELOAD_INTERVAL);
        }
    }

    private void watchLoadedResources() {
        Thread.currentThread().setName("Resource-Watch");
        for (WatchedResource resource : loadedFiles) {
            long lastModified = resource.file.lastModified();
            if (lastModified > resource.lastModified) {
                resource.lastModified = resource.file.lastModified();
                LOG.INFO("Reloading: %s", resource.file.toString());
                try {
                    resource.callback.run();
                } catch (Exception exception) {
                    Exceptions.handle()
                              .withSystemErrorMessage("Error reloading %s: %s (%s)", resource.file.toString())
                              .error(exception)
                              .handle();
                }
            }
        }
    }

    private void startTimer() {
        try {
            timerLock.lock();
            try {
                if (timer != null) {
                    timer.cancel();
                }
                timer = new Timer(true);
                timer.schedule(new InnerTimerTask(), TEN_SECONDS_IN_MILLIS, TEN_SECONDS_IN_MILLIS);
            } finally {
                timerLock.unlock();
            }
        } catch (Exception t) {
            Exceptions.handle(LOG, t);
        }
    }

    @Override
    public void stopped() {
        try {
            timerLock.lock();
            try {
                if (timer != null) {
                    timer.cancel();
                }
            } finally {
                timerLock.unlock();
            }
        } catch (Exception t) {
            Exceptions.handle(LOG, t);
        }
    }

    /**
     * Adds the given file to the list of watched resources in DEVELOP mode ({@link Sirius#isDev()}.
     * <p>
     * This is used to reload files like properties in development environments. In production systems, no
     * reloading will be performed.
     *
     * @param url      the file to watch
     * @param callback the callback to invoke once the file has changed
     */
    @SuppressWarnings("squid:S2250")
    @Explain("Resources are only collected once at startup, so there is no performance hotspot")
    public void addWatchedResource(@Nonnull URL url, @Nonnull Runnable callback) {
        try {
            WatchedResource resource = new WatchedResource();
            File file = new File(url.toURI());
            resource.file = file;
            resource.callback = callback;
            resource.lastModified = file.lastModified();
            loadedFiles.add(resource);
        } catch (IllegalArgumentException | URISyntaxException exception) {
            Exceptions.ignore(exception);
            Exceptions.handle()
                      .withSystemErrorMessage("Cannot monitor URL '%s' for changes: %s (%s)", url)
                      .to(LOG)
                      .handle();
        }
    }

    /**
     * Executes all one minute timers (implementing <tt>EveryTenSeconds</tt>) now (out of schedule).
     */
    public void runTenSecondTimers() {
        for (final TimedTask task : everyTenSeconds.getParts()) {
            executeTask(task);
        }
        lastTenSecondsExecution = timeProvider.currentTimeMillis();
    }

    /**
     * Executes all one minute timers (implementing <tt>EveryMinute</tt>) now (out of schedule).
     */
    public void runOneMinuteTimers() {
        for (final TimedTask task : everyMinute.getParts()) {
            executeTask(task);
        }
        lastOneMinuteExecution = timeProvider.currentTimeMillis();
    }

    private void executeTask(final TimedTask task) {
        tasks.executor(TIMER)
             .dropOnOverload(() -> Exceptions.handle()
                                             .to(LOG)
                                             .withSystemErrorMessage(
                                                     "Dropping timer task '%s' (%s) due to system overload!",
                                                     task,
                                                     task.getClass())
                                             .handle())
             .start(() -> {
                 try {
                     Watch w = Watch.start();
                     task.runTimer();
                     if (w.elapsed(TimeUnit.SECONDS, false) > 1) {
                         LOG.WARN("TimedTask '%s' (%s) took over a second to complete! "
                                  + "Consider executing the work in a separate executor!", task, task.getClass());
                     }
                 } catch (Exception t) {
                     Exceptions.handle(LOG, t);
                 }
             });
    }

    /**
     * Executes all ten minutes timers (implementing <tt>EveryTenMinutes</tt>) now (out of schedule).
     */
    public void runTenMinuteTimers() {
        for (final TimedTask task : everyTenMinutes.getParts()) {
            executeTask(task);
        }
        lastTenMinutesExecution = timeProvider.currentTimeMillis();
    }

    /**
     * Executes all one hour timers (implementing <tt>EveryHour</tt>) now (out of schedule).
     */
    public void runOneHourTimers() {
        for (final TimedTask task : everyHour.getParts()) {
            executeTask(task);
        }
        lastHourExecution = timeProvider.currentTimeMillis();
    }

    /**
     * Executes all daily timers (implementing <tt>EveryDay</tt>) if applicable, or if outOfASchedule is <tt>true</tt>.
     *
     * @param currentHour determines the current hour. Most probably this will be wall-clock time. However, for
     *                    out-of-schedule eecution, this can be set to any value.
     */
    public void runEveryDayTimers(int currentHour) {
        for (final EveryDay task : getDailyTasks()) {
            runDailyTimer(currentHour, task);
        }
    }

    /**
     * Returns all known daily tasks.
     *
     * @return a collection of all known daily tasks
     */
    public Collection<EveryDay> getDailyTasks() {
        return Collections.unmodifiableCollection(everyDay.getParts());
    }

    private void runDailyTimer(int currentHour, EveryDay task) {
        Optional<Integer> executionHour = getExecutionHour(task);
        if (executionHour.isEmpty()) {
            LOG.WARN("Skipping daily timer %s as config key '%s' is missing!",
                     task.getClass().getName(),
                     TIMER_DAILY_PREFIX + task.getConfigKeyName());
            return;
        }

        if (executionHour.get() != currentHour) {
            return;
        }

        if (orchestration != null && !orchestration.shouldRunDailyTask(task.getConfigKeyName())) {
            return;
        }

        executeTask(task);
    }

    /**
     * Determines the execution hour (0..23) in which the given task is to be executed.
     *
     * @param task the task to check
     * @return the execution hour wrapped as optional or an empty optional if the config is missing
     */
    public Optional<Integer> getExecutionHour(EveryDay task) {
        String configPath = TIMER_DAILY_PREFIX + task.getConfigKeyName();
        if (!Sirius.getSettings().getConfig().hasPath(configPath)) {
            return Optional.empty();
        }

        return Optional.of(Sirius.getSettings().getInt(configPath));
    }
}
