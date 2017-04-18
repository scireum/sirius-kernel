/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.timer;

import com.google.common.collect.Lists;
import sirius.kernel.Lifecycle;
import sirius.kernel.Sirius;
import sirius.kernel.async.Tasks;
import sirius.kernel.commons.Watch;
import sirius.kernel.di.PartCollection;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Parts;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;
import sirius.kernel.nls.NLS;

import javax.annotation.Nonnull;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
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
@Register(classes = {Timers.class, Lifecycle.class})
public class Timers implements Lifecycle {

    protected static final Log LOG = Log.get("timer");
    private static final String TIMER = "timer";
    private static final String TIMER_DAILY_PREFIX = "timer.daily.";

    @Part
    private Tasks tasks;

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
    private ReentrantLock timerLock = new ReentrantLock();

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
                if (TimeUnit.MINUTES.convert(System.currentTimeMillis() - lastOneMinuteExecution, TimeUnit.MILLISECONDS)
                    >= 1) {
                    runOneMinuteTimers();
                }
                if (TimeUnit.MINUTES.convert(System.currentTimeMillis() - lastTenMinutesExecution,
                                             TimeUnit.MILLISECONDS) >= 10) {
                    runTenMinuteTimers();
                }
                if (TimeUnit.MINUTES.convert(System.currentTimeMillis() - lastHourExecution, TimeUnit.MILLISECONDS)
                    >= 60) {
                    runOneHourTimers();
                    runEveryDayTimers(false);
                }
            } catch (Throwable t) {
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

    /*
     * Contains the relative paths of all loaded files
     */
    private List<WatchedResource> loadedFiles = Lists.newCopyOnWriteArrayList();

    /*
     * Used to frequently check loaded properties when running in DEVELOP mode.
     */
    private Timer reloadTimer;

    /*
     * Determines the interval which files are checked for update
     */
    private static final int RELOAD_INTERVAL = 1000;

    /**
     * Returns the timestamp of the last execution of the 10 second timer.
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
     * Returns the timestamp of the last execution of the one minute timer.
     *
     * @return a textual representation of the last execution of the one minute timer. Returns "-" if the timer didn't
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
     * Returns the timestamp of the last execution of the one hour timer.
     *
     * @return a textual representation of the last execution of the one hour timer. Returns "-" if the timer didn't
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
                    Thread.currentThread().setName("Resource-Watch");
                    for (WatchedResource res : loadedFiles) {
                        long lastModified = res.file.lastModified();
                        if (lastModified > res.lastModified) {
                            res.lastModified = res.file.lastModified();
                            LOG.INFO("Reloading: %s", res.file.toString());
                            try {
                                res.callback.run();
                            } catch (Exception e) {
                                Exceptions.handle()
                                          .withSystemErrorMessage("Error reloading %s: %s (%s)", res.file.toString())
                                          .error(e)
                                          .handle();
                            }
                        }
                    }
                }
            }, RELOAD_INTERVAL, RELOAD_INTERVAL);
        }
    }

    private void startTimer() {
        try {
            timerLock.lock();
            try {
                if (timer == null) {
                    timer = new Timer(true);
                } else {
                    timer.cancel();
                    timer = new Timer(true);
                }
                timer.schedule(new InnerTimerTask(), 1000 * 10, 1000 * 10);
            } finally {
                timerLock.unlock();
            }
        } catch (Throwable t) {
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
        } catch (Throwable t) {
            Exceptions.handle(LOG, t);
        }
    }

    @Override
    public void awaitTermination() {
        // Not necessary
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
    public void addWatchedResource(@Nonnull URL url, @Nonnull Runnable callback) {
        try {
            WatchedResource res = new WatchedResource();
            File file = new File(url.toURI());
            res.file = file;
            res.callback = callback;
            res.lastModified = file.lastModified();
            loadedFiles.add(res);
        } catch (IllegalArgumentException | URISyntaxException e) {
            Exceptions.ignore(e);
            Exceptions.handle()
                      .withSystemErrorMessage("Cannot monitor URL '%s' for changes: %s (%s)", url)
                      .to(LOG)
                      .handle();
        }
    }

    @Override
    public String getName() {
        return "timer (System Timer Services)";
    }

    /**
     * Executes all one minute timers (implementing <tt>EveryTenSeconds</tt>) now (out of schedule).
     */
    public void runTenSecondTimers() {
        for (final TimedTask task : everyTenSeconds.getParts()) {
            executeTask(task);
        }
        lastTenSecondsExecution = System.currentTimeMillis();
    }

    /**
     * Executes all one minute timers (implementing <tt>EveryMinute</tt>) now (out of schedule).
     */
    public void runOneMinuteTimers() {
        for (final TimedTask task : everyMinute.getParts()) {
            executeTask(task);
        }
        lastOneMinuteExecution = System.currentTimeMillis();
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
                 } catch (Throwable t) {
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
        lastTenMinutesExecution = System.currentTimeMillis();
    }

    /**
     * Executes all one hour timers (implementing <tt>EveryHour</tt>) now (out of schedule).
     */
    public void runOneHourTimers() {
        for (final TimedTask task : everyHour.getParts()) {
            executeTask(task);
        }
        lastHourExecution = System.currentTimeMillis();
    }

    /**
     * Executes all daily timers (implementing <tt>EveryDay</tt>) if applicable, or if outOfASchedule is <tt>true</tt>.
     *
     * @param outOfSchedule determines if the 'timers.daily.[configKeyName]' should be checked if now is the right
     *                      hour of day to execute this task, or if it should be executed in any case (<tt>true</tt>).
     */
    public void runEveryDayTimers(boolean outOfSchedule) {
        for (final EveryDay task : everyDay.getParts()) {
            if (!Sirius.getSettings().getConfig().hasPath(TIMER_DAILY_PREFIX + task.getConfigKeyName())) {
                LOG.WARN("Skipping daily timer %s as config key '%s' is missing!",
                         task.getClass().getName(),
                         TIMER_DAILY_PREFIX + task.getConfigKeyName());
            } else {
                if (outOfSchedule
                    || Sirius.getSettings().getInt(TIMER_DAILY_PREFIX + task.getConfigKeyName()) == LocalTime.now()
                                                                                                             .getHour()) {
                    executeTask(task);
                }
            }
        }
    }
}
