/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health.metrics;

import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import sirius.kernel.Sirius;
import sirius.kernel.async.CallContext;
import sirius.kernel.commons.Monoflop;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;
import sirius.kernel.health.MemoryBasedHealthMonitor;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.List;

/**
 * Provides core metrics for the operating system, the Java Virtual Machine and central frameworks.
 */
@Register
public class SystemMetricProvider implements MetricProvider {

    private List<GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();
    private List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
    private Sigar sigar = new Sigar();
    private volatile boolean sigarEnabled = true;
    private Monoflop openFilesChecked = Monoflop.create();
    private static final Log LOG = Log.get("sigar");

    @Part
    private MemoryBasedHealthMonitor monitor;

    @ConfigValue("health.minimalOpenFilesLimit")
    private long minimalOpenFilesLimit;

    @Override
    public void gather(MetricsCollector collector) {
        gatherMemoryMetrics(collector);
        gatherGCMetrics(collector);
        gatherOSMetrics(collector);

        collector.differentialMetric("sys-interactions",
                                     "sys-interactions",
                                     "Interactions",
                                     CallContext.getInteractionCounter().getCount(),
                                     "/min");
        collector.differentialMetric("sys-logs", "sys-logs", "Log Messages", monitor.getNumLogMessages(), "/min");
        collector.differentialMetric("sys-incidents", "sys-incidents", "Incidents", monitor.getNumIncidents(), "/min");
        collector.differentialMetric("sys-unique-incidents",
                                     "sys-unique-incidents",
                                     "Unique Incidents",
                                     monitor.getNumUniqueIncidents(),
                                     "/min");

        collector.metric("sys-log-size",
                         "Log files size",
                         Sirius.getSetup().estimateLogFilesSize() / 1024d / 1024d,
                         "MB");
    }

    private void gatherOSMetrics(MetricsCollector collector) {
        try {
            if (sigarEnabled) {
                gatherCPUandMem(collector);
                gatherNetworkStats(collector);
                gatherFS(collector);
                checkMaxNumberOfOpenFiles();
            }
        } catch (SigarException e) {
            Exceptions.handle(LOG, e);
        } catch (UnsatisfiedLinkError e) {
            Exceptions.ignore(e);
            sigarEnabled = false;
        }
    }

    /*
     * We check the max number of open files for the underlying system as this is commonly too
     * low on many linux machines and causes ugly errors.
     *
     * As we rely on sigar anyway and don't want to risk an link error / exception on startup, we check
     * this value on the first run of the metrics collector
     */
    private void checkMaxNumberOfOpenFiles() throws SigarException {
        if (openFilesChecked.firstCall()) {
            long maxOpenFiles = sigar.getResourceLimit().getOpenFilesMax();
            if (maxOpenFiles > 0 && minimalOpenFilesLimit > 0 && maxOpenFiles < minimalOpenFilesLimit) {
                Exceptions.handle()
                          .withSystemErrorMessage(
                                  "The ulimit -f (number of open files) is too low: %d - It should be at least: %d",
                                  maxOpenFiles,
                                  minimalOpenFilesLimit)
                          .to(LOG)
                          .handle();
            } else {
                LOG.INFO("The maximal number of open files on this system is good (%d, Required are at least: %d)",
                         maxOpenFiles,
                         minimalOpenFilesLimit);
            }
        }
    }

    private void gatherGCMetrics(MetricsCollector collector) {
        for (GarbageCollectorMXBean gc : gcs) {
            collector.differentialMetric("jvm-gc-" + gc.getName(),
                                         "jvm-gc",
                                         "GC - " + gc.getName(),
                                         gc.getCollectionCount(),
                                         "/min");
        }
    }

    private void gatherMemoryMetrics(MetricsCollector collector) {
        for (MemoryPoolMXBean pool : pools) {
            if (pool.getName().toLowerCase().contains("old") && pool.getUsage().getMax() > 0) {
                collector.metric("jvm-old-heap",
                                 "JVM Heap (" + pool.getName() + ")",
                                 100d * pool.getUsage().getUsed() / pool.getUsage().getMax(),
                                 "%");
            }
        }
    }

    private void gatherNetworkStats(MetricsCollector collector) throws SigarException {
        long rxSum = 0;
        long txSum = 0;
        for (String eth : sigar.getNetInterfaceList()) {
            NetInterfaceStat stat = sigar.getNetInterfaceStat(eth);
            rxSum += stat.getRxBytes();
            txSum += stat.getTxBytes();
        }
        collector.differentialMetric("sys-eth-tx", "sys-eth-tx", "Network Bytes-Out", txSum / 1024d / 60, "KB/s");
        collector.differentialMetric("sys-eth-rx", "sys-eth-rx", "Network Bytes-In", rxSum / 1024d / 60, "KB/s");
    }

    private void gatherCPUandMem(MetricsCollector collector) throws SigarException {
        CpuPerc cpu = sigar.getCpuPerc();
        collector.metric("sys-cpu", "System CPU Usage", cpu.getCombined() * 100d, "%");
        Mem mem = sigar.getMem();
        mem.gather(sigar);
        collector.metric("sys-mem", "System Memory Usage", mem.getUsedPercent(), "%");
        ProcCpu proc = sigar.getProcCpu(sigar.getPid());
        collector.metric("jvm-cpu", "JVM CPU Usage", proc.getPercent(), "%");
        Runtime rt = Runtime.getRuntime();
        collector.metric("jvm-heap",
                         "JVM Heap Usage",
                         (double) (rt.totalMemory() - rt.freeMemory()) / rt.maxMemory() * 100d,
                         "%");
    }

    private void gatherFS(MetricsCollector collector) throws SigarException {
        for (FileSystem fs : sigar.getFileSystemList()) {
            if (fs.getType() == FileSystem.TYPE_LOCAL_DISK) {
                FileSystemUsage fsu = sigar.getMountedFileSystemUsage(fs.getDirName());
                collector.metric("sys-fs", "FS: Usage of " + fs.getDirName(), fsu.getUsePercent() * 100d, "%");
            }
        }
    }
}
