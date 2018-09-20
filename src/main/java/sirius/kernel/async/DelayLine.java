/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Counter;
import sirius.kernel.health.metrics.MetricProvider;
import sirius.kernel.health.metrics.MetricsCollector;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Permits to delay the execution of a task by a certain amount of seconds.
 * <p>
 * Provides a background queue, which is checked about every 500ms. All tasks,
 * which delay is expired will be invoked in the given executor.
 * <p>
 * Note that this should not be used with large delays, but rather for short durations (e.g. less than 60 seconds,
 * although this depends heavily on the use case, as there is no inherent limit other than tasks won't survive a
 * system restart).
 */
@Register(classes = {DelayLine.class, BackgroundLoop.class, MetricProvider.class})
public class DelayLine extends BackgroundLoop implements MetricProvider {

    private static class WaitingTask implements Runnable {
        long timeout;
        String executor;
        Runnable task;

        @Override
        public void run() {
            task.run();
        }
    }

    private final List<WaitingTask> waitingTasks = new ArrayList<>();
    private Counter backgroundTasks = new Counter();

    @Part
    private Tasks tasks;

    /**
     * Queues the given task to be called after roughly the number of seconds given here.
     *
     * @param executor       the executor to execute the task in. Use {@link Tasks#DEFAULT} is no other appropriate
     *                       pool
     *                       is available.
     * @param delayInSeconds the number to wait in seconds. Note that the delay can be a bit longer, depending on the
     *                       system load.
     * @param task           the task to execute. Note that the {@link CallContext} isn't transferred to the task being
     *                       invoked. Use {@link #forkDelayed(String, long, Runnable)} if you need the context.
     */
    public void callDelayed(@Nonnull String executor, long delayInSeconds, @Nonnull Runnable task) {
        synchronized (waitingTasks) {
            WaitingTask waitingTask = new WaitingTask();
            waitingTask.executor = executor;
            waitingTask.timeout = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(delayInSeconds);
            waitingTask.task = task;
            waitingTasks.add(waitingTask);
        }
    }

    /**
     * Queues the given task to be called after roughly the number of seconds given here.
     * <p>
     * In contrast to {@link #callDelayed(String, long, Runnable)}, this will preserve the {@link CallContext} when
     * invoking the <tt>task</tt>.
     * </p>
     *
     * @param executor       the executor to execute the task in. Use {@link Tasks#DEFAULT} is no other appropriate
     *                       pool
     *                       is available.
     * @param delayInSeconds the number to wait in seconds. Note that the delay can be a bit longer, depending on the
     *                       system load.
     * @param task           the task to execute
     */
    public void forkDelayed(@Nonnull String executor, long delayInSeconds, @Nonnull Runnable task) {
        CallContext currentContext = CallContext.getCurrent();
        callDelayed(executor, delayInSeconds, () -> {
            CallContext backup = CallContext.getCurrent();
            try {
                CallContext.setCurrent(currentContext);
                task.run();
            } finally {
                CallContext.setCurrent(backup);
            }
        });
    }

    @Nonnull
    @Override
    public String getName() {
        return "DelayLine";
    }

    @Override
    public double maxCallFrequency() {
        return 2;
    }

    @Override
    protected String doWork() throws Exception {
        long now = System.currentTimeMillis();
        int numScheduled = 0;
        synchronized (waitingTasks) {
            Iterator<WaitingTask> iter = waitingTasks.iterator();
            while (iter.hasNext()) {
                WaitingTask next = iter.next();
                if (next.timeout > now) {
                    return numScheduled > 0 ? "Re-Scheduled Tasks: " + numScheduled : null;
                }

                iter.remove();
                tasks.executor(next.executor).start(next);
                numScheduled++;
            }
        }

        return null;
    }

    @Override
    public void gather(MetricsCollector collector) {
        if (backgroundTasks.getCount() > 0) {
            collector.differentialMetric("delay-line-tasks",
                                         "delay-line-tasks",
                                         "Delay-Line Tasks",
                                         backgroundTasks.getCount(),
                                         "1/min");
        }

        long length;
        synchronized (waitingTasks) {
            length = waitingTasks.size();
        }
        collector.metric("delay-line-length", "Delay-Line Length", length, null);
    }
}
