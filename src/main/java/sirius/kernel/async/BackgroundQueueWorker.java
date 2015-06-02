/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

import sirius.kernel.commons.Wait;
import sirius.kernel.commons.Watch;
import sirius.kernel.health.Exceptions;

import java.time.Duration;

/**
 * Executes a {@link BackgroundTaskQueue}
 */
class BackgroundQueueWorker implements Runnable {

    private static final Duration SLEEP_INTERVAL = Duration.ofSeconds(15);

    private BackgroundTaskQueue tq;
    private Watch w = Watch.start();
    private volatile boolean active;

    protected BackgroundQueueWorker(BackgroundTaskQueue tq) {
        this.tq = tq;
    }

    protected BackgroundQueueWorker start() {
        Thread t = new Thread(this);
        t.setName("BackgroundQueue-" + tq.getQueueName());
        t.start();
        return this;
    }

    @Override
    public void run() {
        TaskContext tc = TaskContext.get();
        tc.setSystem("BackgroundQueue").setSubSystem(tq.getQueueName());
        while (tc.isActive()) {
            w.reset();
            Runnable work = tq.getWork();
            if (work != null) {
                try {
                    active = true;
                    work.run();
                    w.submitMicroTiming("BACKGROUND", tq.getQueueName());
                    active = false;
                } catch (Throwable e) {
                    Exceptions.handle()
                              .to(Async.LOG)
                              .error(e)
                              .withSystemErrorMessage(
                                      "Error while processing work from a background queue '%s': %s (%s)",
                                      tq.getQueueName())
                              .handle();
                }
            } else {
                Wait.duration(SLEEP_INTERVAL);
            }
        }
    }

    /**
     * Returns the state of the background worker as string
     *
     * @return a string representation of the current state of the worker
     */
    public String getState() {
        if (active) {
            return "ACTIVE: " + w.duration();
        } else {
            return "INACTIVE: " + w.duration();
        }
    }

    @Override
    public String toString() {
        return "BackgroundQueue-" + tq.getQueueName() + " - " + getState();
    }
}
