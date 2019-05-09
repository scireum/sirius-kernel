/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health.metrics;

import sirius.kernel.async.BackgroundLoop;
import sirius.kernel.async.CallContext;
import sirius.kernel.di.PartCollection;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Parts;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Log;
import sirius.kernel.health.MemoryBasedHealthMonitor;
import sirius.kernel.nls.NLS;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Provides core metrics for the operating system, the Java Virtual Machine and central frameworks.
 */
@Register
public class SystemMetricProvider implements MetricProvider {

    private List<GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();
    private List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();

    @Part
    private MemoryBasedHealthMonitor monitor;

    @Parts(BackgroundLoop.class)
    private PartCollection<BackgroundLoop> loops;

    @Override
    public void gather(MetricsCollector collector) {
        gatherMemoryMetrics(collector);
        gatherGCMetrics(collector);

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

        gatherBlockingLoops(collector);
    }

    private void gatherBlockingLoops(MetricsCollector collector) {
        int blockingLoops = 0;
        Instant now = Instant.now();
        for (BackgroundLoop loop : loops) {
            long observedRuntimeSeconds = Duration.between(loop.getLastExecutionAttempt(), now).getSeconds();
            if (observedRuntimeSeconds > loop.maxRuntimeInSeconds()) {
                blockingLoops++;
                Log.BACKGROUND.WARN("BackgroundLoop %s has exceeded its runtime! Expected runtime %ss."
                                    + " Observed runtime: %ss."
                                    + " Last execution attempt: %s",
                                    loop.getName(),
                                    loop.maxRuntimeInSeconds(),
                                    observedRuntimeSeconds,
                                    NLS.toUserString(loop.getLastExecutionAttempt()));
            }
        }

        collector.metric("blocked-loops",
                         "Blocked Background Loops",
                         blockingLoops,
                         null,
                         blockingLoops > 0 ? MetricState.RED : MetricState.GRAY);
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
        reportHeapUsage(collector);
        reportOffHeapUsage(collector);

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

    private void reportHeapUsage(MetricsCollector collector) {
        MemoryUsage heapMemoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        collector.metric("jvm-heap",
                         "JVM Heap",
                         heapMemoryUsage.getUsed() * 100d / heapMemoryUsage.getMax(),
                         "%",
                         MetricState.GREEN);
        collector.metric("jvm-heap-max",
                         "JVM Heap Max",
                         heapMemoryUsage.getMax() / 1024d / 1024d,
                         "MB",
                         MetricState.GRAY);
    }

    private void reportOffHeapUsage(MetricsCollector collector) {
        long memoryUsage = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed();
        collector.metric("jvm-non-heap", "JVM Non Heap", memoryUsage / 1024d / 1024d, "MB", MetricState.GRAY);
    }
}
