/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

import com.google.common.collect.Lists;
import sirius.kernel.commons.Watch;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.metrics.MetricProvider;
import sirius.kernel.health.metrics.MetricsCollector;
import sirius.kernel.nls.NLS;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Tracks the execution of a blocking operation.
 * <p>
 * The operations framework is used to track blocking operations which might possibly hang. As a blocking operation
 * cannot be checked by the calling thread itself, the classical alternative would be to fork a thread which
 * monitors the operation. As this approach does not scale very well, the operations framework creates a
 * lighweight <tt>Operation</tt> object around a potentially blocking operation using a try-with-resources block.
 * <p>
 * A metrics provider will check for all operations and use its limits (set by <tt>component-kernel.conf</tt>,
 * to warn if too many operations are active (or are probably hanging).
 * <p>
 * Other frameworks can provider further help: <tt>SIRIUS-WEB</tt> e.g. provides a list of all operations
 * using the <i>async</i> command in the system console.
 */
public class Operation implements AutoCloseable {

    private static final Set<Operation> ops =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private Supplier<String> nameProvider;
    private Duration timeout;
    private String name;
    private Watch w = Watch.start();

    /**
     * Provides a legacy support for as long as sirius-search 1.x and 2.x are around.
     */
    @Deprecated
    public static void cover(String category, Supplier<String> name, Duration timeout, Runnable block) {
        try(Operation op = new Operation(name, timeout)) {
            block.run();
        }
    }

    /**
     * Creates a new operation.
     *
     * @param name    the supplier used to compute a user readable name if the operation is rendered somewhere
     * @param timeout the timeout. If the duration is longer than the given timeout,
     *                this operation is considered "hanging"
     */
    public Operation(Supplier<String> name, Duration timeout) {
        this.nameProvider = name;
        this.timeout = timeout;
        ops.add(this);
    }

    @Override
    public void close() {
        ops.remove(this);
        if (isOvertime()) {
            Tasks.LOG.WARN(toString());
        }
    }

    /**
     * Determines if the duration of the operation is longer than its timeout
     *
     * @return <tt>true</tt> if the duration of the operation is longer than its timeout, <tt>false</tt> otherwise
     */
    public boolean isOvertime() {
        return w.elapsed(TimeUnit.SECONDS, false) > timeout.getSeconds();
    }

    @Override
    public String toString() {
        if (name == null) {
            name = nameProvider.get();
        }

        String result = name + " (" + w.duration() + "/" + NLS.convertDuration(timeout.getSeconds(), true, false) + ")";

        if (isOvertime()) {
            result += " OVERTIME!";
        }
        return result;
    }

    /**
     * Returns a list of all currently active operations
     *
     * @return a list of all known operations
     */
    public static List<Operation> getActiveOperations() {
        return Lists.newArrayList(ops);
    }

    /**
     * Provides metrics of the operation monitoring.
     * <p>
     * The provided metrics are <tt>active-operations</tt>, which contains the number of active operations and
     * <tt>hanging-operations</tt>, which contains the number of operations that take longer than expected
     * (and therefore might hang).
     */
    @Register
    public static class OperationMetrics implements MetricProvider {

        @Override
        public void gather(MetricsCollector collector) {
            collector.metric("active-operations", "Active-Operations", ops.size(), null);
            collector.metric("hanging-operations",
                             "Hanging-Operations",
                             getActiveOperations().stream().filter(Operation::isOvertime).count(),
                             null);
        }
    }
}
