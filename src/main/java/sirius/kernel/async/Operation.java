/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import sirius.kernel.commons.Lambdas;
import sirius.kernel.commons.Watch;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.metrics.MetricProvider;
import sirius.kernel.health.metrics.MetricsCollector;
import sirius.kernel.nls.NLS;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
 * lighweight <tt>Operation</tt> object before a block operation is started using
 * {@link Operation#create(String, Supplier, Duration)}. Once the operation is completed the framework is
 * notified using {@link Operation#release(Operation)}.
 * <p>
 * A metrics provider will check for all operations and use its limits (set by <tt>component-kernel.conf</tt>,
 * to warn if too many operations are active (or are probably hanging).
 * <p>
 * Other frameworks can provider further help: <tt>SIRIUS-WEB</tt> e.g. provides a list of all operations
 * using the <i>async</i> command in the system console.
 * <p>
 * To remove the runtime overhead, operations can be enabled or disabled by category using the system
 * configuration. By default all categories are enabled.
 */
public class Operation {

    private static Set<String> categories = null;
    private static final Set<Operation> ops =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    @ConfigValue("async.operations.categories")
    private static List<String> enabledCategories;

    /**
     * Creates a new operation for the given category.
     *
     * @param category the categores used to determine if monitoring is enabled or not
     * @param name     the supplier used to compute a user readable name if the operation is rendered somewhere
     * @param timeout  the timeout. If the duration is longer than the given timeout,
     *                 this operation is considered "hanging"
     * @return a operation object representing the described operation or <tt>null</tt> if monitoring for the
     * given category is disabled
     */
    public static Operation create(@Nonnull String category,
                                   @Nonnull Supplier<String> name,
                                   @Nonnull Duration timeout) {
        if (categories == null) {
            synchronized (ops) {
                categories =
                        enabledCategories.stream().map(String::intern).collect(Lambdas.into(Sets.newIdentityHashSet()));
            }
        }
        if (categories.isEmpty() || categories.contains(category)) {
            Operation op = new Operation(name, timeout);
            synchronized (op) {
                ops.add(op);
            }
            return op;
        } else {
            return null;
        }
    }

    /**
     * Releases an operation which was previously created using <tt>create</tt>.
     * <p>
     * It is usually a good practive to place this in an <b>finally</b> block after the blocking operation.
     *
     * @param op the operation to release. Can be <tt>null</tt> as <tt>create</tt> returns this for non-monitored
     *           operations
     */
    public static void release(@Nullable Operation op) {
        if (op != null) {
            synchronized (ops) {
                ops.remove(op);
            }
        }
    }

    /**
     * Boilerpalte method to cover a single call in an <tt>Operation</tt>.
     *
     * @param category  the categores used to determine if monitoring is enabled or not
     * @param name      the supplier used to compute a user readable name if the operation is rendered somewhere
     * @param timeout   the timeout. If the duration is longer than the given timeout,
     *                  this operation is considered "hanging"
     * @param operation the actual call to wrap in an operation
     */
    public static void cover(@Nonnull String category,
                             @Nonnull Supplier<String> name,
                             @Nonnull Duration timeout,
                             Runnable operation) {
        Operation op = create(category, name, timeout);
        try {
            operation.run();
        } finally {
            release(op);
        }
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

    private Supplier<String> nameProvider;
    private Duration timeout;
    private String name;
    private Watch w = Watch.start();

    private Operation(Supplier<String> nameProvider, Duration timeout) {
        this.nameProvider = nameProvider;
        this.timeout = timeout;
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
}
