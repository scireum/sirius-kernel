/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health.metrics;

import sirius.kernel.Sirius;
import sirius.kernel.async.CallContext;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.MemoryBasedHealthMonitor;

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.util.List;

/**
 * Provides core metrics for the operating system, the Java Virtual Machine and central frameworks.
 */
@Register
public class SystemMetricProvider implements MetricProvider {

    private static final String OSX_DEV_FS = "devfs";

    private List<GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();
    private List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();

    @Part
    private MemoryBasedHealthMonitor monitor;

    @Override
    public void gather(MetricsCollector collector) {
        gatherMemoryMetrics(collector);
        gatherGCMetrics(collector);
        gatherFS(collector);

        collector.differentialMetric("kernel_interactions",
                                     "sys-interactions",
                                     "Interactions",
                                     CallContext.getInteractionCounter().getCount(),
                                     "/min");
        collector.differentialMetric("kernel_log_entries",
                                     "sys-logs",
                                     "Log Messages",
                                     monitor.getNumLogMessages(),
                                     "/min");
        collector.differentialMetric("kernel_incidents",
                                     "sys-incidents",
                                     "Incidents",
                                     monitor.getNumIncidents(),
                                     "/min");
        collector.differentialMetric("kernel_unique_incidents",
                                     "sys-unique-incidents",
                                     "Unique Incidents",
                                     monitor.getNumUniqueIncidents(),
                                     "/min");

        collector.metric("sys-log-size",
                         "Log files size",
                         Sirius.getSetup().estimateLogFilesSize() / 1024d / 1024d,
                         "MB");
    }

    private void gatherGCMetrics(MetricsCollector collector) {
        for (GarbageCollectorMXBean gc : gcs) {
            collector.differentialMetric("jvm_gc_" + gc.getName().toLowerCase(),
                                         "jvm-gc",
                                         "GC - " + gc.getName(),
                                         gc.getCollectionCount(),
                                         "/min");
        }
    }

    private void gatherMemoryMetrics(MetricsCollector collector) {
        for (MemoryPoolMXBean pool : pools) {
            if (pool.getName().toLowerCase().contains("old") && pool.getUsage().getMax() > 0) {
                collector.metric("jvm_old_heap",
                                 "jvm-old-heap",
                                 "JVM Heap (" + pool.getName() + ")",
                                 100d * pool.getUsage().getUsed() / pool.getUsage().getMax(),
                                 "%");
            }
        }
    }

    private void gatherFS(MetricsCollector collector) {
        for (FileStore store : FileSystems.getDefault().getFileStores()) {
            try {
                // OSX stupidly reports "/dev" as "devfs" which is always 100% utilized...
                // Also Linux reports some virtual file systems without a size.
                if (!Strings.areEqual(store.name(), OSX_DEV_FS) && store.getTotalSpace() > 0) {
                    double usage = 100d - (100d * store.getUsableSpace() / store.getTotalSpace());
                    collector.metric("sys-fs", "FS: Usage of " + store.name(), usage, "%");
                }
            } catch (IOException e) {
                Exceptions.ignore(e);
            }
        }
    }
}
