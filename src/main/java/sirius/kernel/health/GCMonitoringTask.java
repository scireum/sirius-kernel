/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health;

import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.di.std.Register;
import sirius.kernel.timer.EveryTenSeconds;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

/**
 * Provides a simple monitoring task which can log "long" GC pauses.
 * <p>
 * Note that this is sort of a tool for the desperate and thus disabled by default (the GC is expected to run every once
 * in a while and to also take its time depending on the heap). An excessive GC load is already detected by the
 * {@link sirius.kernel.health.metrics.SystemMetricProvider}. This monitor however can be used trying to identify
 * if lags in the application can be correlated with longer GC pauses.
 * <p>
 * To enable this monitor, the <b>gc</b> logger must be set to <tt>FINE</tt>. The threshold which pauses to log
 * can be set in the system config via <tt>health.gcPauseLoggingThreshold</tt>.
 */
@Register
public class GCMonitoringTask implements EveryTenSeconds {

    private static final Log LOG = Log.get("gc");

    @ConfigValue("health.gcPauseLoggingThreshold")
    private long gcPauseLoggingThreshold;

    private volatile long lastGCTime = 0;
    private final List<GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();

    @Override
    public void runTimer() throws Exception {
        long collectionTimeMillis = 0;
        for (GarbageCollectorMXBean gc : gcs) {
            collectionTimeMillis += gc.getCollectionTime();
        }

        if (LOG.isFINE()) {
            long gcPause = collectionTimeMillis - lastGCTime;
            if (gcPause >= gcPauseLoggingThreshold) {
                LOG.FINE("A cumulative GC pause of %s ms during the last 10 seconds was detected.", gcPause);
            }
        }

        lastGCTime = collectionTimeMillis;
    }
}
