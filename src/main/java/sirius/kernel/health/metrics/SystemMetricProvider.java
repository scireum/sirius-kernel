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
import sirius.kernel.xml.Outcall;
import sirius.kernel.xml.SOAPClient;

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

    private final List<GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();
    private final List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
    private volatile long lastGCMeasurement = 0;
    private volatile long lastGCTime = 0;

    @Part
    private MemoryBasedHealthMonitor monitor;

    @Parts(BackgroundLoop.class)
    private PartCollection<BackgroundLoop> loops;

    @Override
    public void gather(MetricsCollector collector) {
        gatherMemoryMetrics(collector);
        gatherGCMetrics(collector);
        gatherFrameworkMetrics(collector);
        gatherBlockingLoops(collector);
        gatherOutcallMetrics(collector);
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
                                 Metric.UNIT_PERCENT);
            }
        }
    }

    private void reportHeapUsage(MetricsCollector collector) {
        MemoryUsage heapMemoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        collector.metric("jvm-heap",
                         "JVM Heap",
                         heapMemoryUsage.getUsed() * 100d / heapMemoryUsage.getMax(),
                         Metric.UNIT_PERCENT,
                         MetricState.GREEN);
        collector.metric("jvm-heap-max",
                         "JVM Heap Max",
                         Metric.bytesToMebibytes(heapMemoryUsage.getMax()),
                         Metric.UNIT_MIB,
                         MetricState.GRAY);
    }

    private void reportOffHeapUsage(MetricsCollector collector) {
        long memoryUsage = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed();
        collector.metric("jvm-non-heap",
                         "JVM Non Heap",
                         Metric.bytesToMebibytes(memoryUsage),
                         Metric.UNIT_MIB,
                         MetricState.GRAY);
    }

    private void gatherGCMetrics(MetricsCollector collector) {
        long collectionTimeMillis = 0;
        long collectionRuns = 0;
        for (GarbageCollectorMXBean gc : gcs) {
            collectionTimeMillis += gc.getCollectionTime();
            collectionRuns += gc.getCollectionCount();
        }

        collector.differentialMetric("jvm_gc_runs", "jvm-gc-runs", "GCs ", collectionRuns, Metric.UNIT_PER_MIN);

        if (lastGCMeasurement > 0) {
            long wallClockTimeMillis = System.currentTimeMillis() - lastGCMeasurement;
            long gcUtilization = 100 * (collectionTimeMillis - lastGCTime) / wallClockTimeMillis;

            collector.differentialMetric("jvm_gc_utilization",
                                         "jvm-gc-utilization",
                                         "GC Utilization",
                                         gcUtilization,
                                         Metric.UNIT_PERCENT);
        }

        lastGCTime = collectionTimeMillis;
        lastGCMeasurement = System.currentTimeMillis();
    }

    private void gatherFrameworkMetrics(MetricsCollector collector) {
        collector.differentialMetric("kernel_interactions",
                                     "sys-interactions",
                                     "Interactions",
                                     CallContext.getInteractionCounter().getCount(),
                                     Metric.UNIT_PER_MIN);
        collector.differentialMetric("kernel_log_entries",
                                     "sys-logs",
                                     "Log Messages",
                                     monitor.getNumLogMessages(),
                                     Metric.UNIT_PER_MIN);
        collector.differentialMetric("kernel_incidents",
                                     "sys-incidents",
                                     "Incidents",
                                     monitor.getNumIncidents(),
                                     Metric.UNIT_PER_MIN);
        collector.differentialMetric("kernel_unique_incidents",
                                     "sys-unique-incidents",
                                     "Unique Incidents",
                                     monitor.getNumUniqueIncidents(),
                                     Metric.UNIT_PER_MIN);
    }

    private void gatherBlockingLoops(MetricsCollector collector) {
        int blockingLoops = 0;
        Instant now = Instant.now();
        for (BackgroundLoop loop : loops) {
            long observedRuntimeSeconds = Duration.between(loop.getLastExecutionAttempt(), now).getSeconds();
            if (loop.isExecuting() && observedRuntimeSeconds > loop.maxRuntimeInSeconds()) {
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

    private void gatherOutcallMetrics(MetricsCollector collector) {
        collector.metric("outcall_count",
                         "sys-outcall-count",
                         "Outcall: Number of requests",
                         Outcall.getTimeToFirstByte().getCount(),
                         Metric.UNIT_PER_MIN);
        collector.metric("outcall_avg_ttfb",
                         "sys-outcall-avg-ttfb",
                         "Outcall: Avg. time to first byte",
                         Outcall.getTimeToFirstByte().getAndClear(),
                         Metric.UNIT_MS);

        collector.metric("soap_call_count",
                         "sys-soapcall-count",
                         "SOAP: Number of calls",
                         SOAPClient.getResponseTime().getCount(),
                         Metric.UNIT_PER_MIN);
        collector.metric("soap_avg_response_time",
                         "sys-soap-avg-response-time",
                         "SOAP: Avg. response time",
                         SOAPClient.getResponseTime().getAndClear(),
                         Metric.UNIT_MS);
    }
}
