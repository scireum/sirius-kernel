/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health.metrics;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import sirius.kernel.Sirius;
import sirius.kernel.async.Tasks;
import sirius.kernel.commons.DataCollector;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Parts;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;
import sirius.kernel.timer.EveryMinute;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Collects and stores metrics for various parts of the system.
 * <p>
 * To provide custom metrics, implement {@link MetricProvider} and register it in the component model using the
 * {@link Register} annotation.
 * <p>
 * The collected metrics are updated once every minute.
 */
@Register(classes = {Metrics.class, EveryMinute.class})
public class Metrics implements EveryMinute {

    private static final String HEALTH_LIMITS_PREFIX = "health.limits.";
    private static final String LIMIT_TYPE_GRAY = ".gray";
    private static final String LIMIT_TYPE_WARNING = ".warning";
    private static final String LIMIT_TYPE_ERROR = ".error";

    @Parts(MetricProvider.class)
    private Collection<MetricProvider> providers;

    /**
     * Contains all limits as defined in the system config
     */
    private Map<String, Limit> limits = Maps.newHashMap();

    /**
     * Contains the last value of each metric in order to compute differential metrics
     */
    private Map<String, Double> differentials = Maps.newHashMap();

    @Part
    private Tasks tasks;

    /**
     * Contains all collected metrics
     */
    private List<Metric> metricsList = Lists.newArrayList();

    /**
     * Internal structure to combine the three limits available for each metric: gray, warning (yellow), error (red).
     */
    private static class Limit {
        double gray = 0;
        double yellow = 0;
        double red = 0;

        @Override
        public String toString() {
            return "(" + gray + " / " + yellow + " / " + red + ")";
        }
    }

    /**
     * Provides an adapter from DataCollector to MetricsCollector.
     */
    private class MetricCollectorAdapter implements MetricsCollector {

        private DataCollector<Metric> collector;

        private MetricCollectorAdapter(DataCollector<Metric> collector) {
            this.collector = collector;
        }

        @Override
        public void metric(String title, double value, String unit, MetricState state) {
            collector.add(new Metric(title, value, state, unit));
        }

        @Override
        public void metric(String limitType, String title, double value, String unit) {
            collector.add(new Metric(title, value, computeState(limitType, value), unit));
        }

        @Override
        public void differentialMetric(String id, String limitType, String title, double currentValue, String unit) {
            Double lastValue = differentials.get(id);
            if (lastValue != null) {
                metric(limitType, title, currentValue - lastValue, unit);
            }
            differentials.put(id, currentValue);
        }

        /*
         * Computes the state of the metric based in the limits given in the config
         */
        private MetricState computeState(String limitType, double value) {
            Limit limit = limits.computeIfAbsent(limitType, this::loadLimit);

            if (value <= limit.gray) {
                return MetricState.GRAY;
            }
            if (limit.red > 0 && value >= limit.red) {
                return MetricState.RED;
            }
            if (limit.yellow > 0 && value >= limit.yellow) {
                return MetricState.YELLOW;
            }
            return MetricState.GREEN;
        }

        private Limit loadLimit(String limitType) {
            Limit limit = new Limit();
            String configPrefix = HEALTH_LIMITS_PREFIX + limitType;
            if (Sirius.getSettings().getConfig().hasPath(configPrefix + LIMIT_TYPE_GRAY)) {
                limit.gray = Sirius.getSettings().getConfig().getDouble(configPrefix + LIMIT_TYPE_GRAY);
            }
            if (Sirius.getSettings().getConfig().hasPath(configPrefix + LIMIT_TYPE_WARNING)) {
                limit.yellow = Sirius.getSettings().getConfig().getDouble(configPrefix + LIMIT_TYPE_WARNING);
            }
            if (Sirius.getSettings().getConfig().hasPath(configPrefix + ".warn")) {
                Log.SYSTEM.WARN("Invalid metrics limit: '%s' - Use: '%s' instead",
                                configPrefix + ".warn",
                                configPrefix + LIMIT_TYPE_WARNING);
            }
            if (Sirius.getSettings().getConfig().hasPath(configPrefix + LIMIT_TYPE_ERROR)) {
                limit.red = Sirius.getSettings().getConfig().getDouble(configPrefix + LIMIT_TYPE_ERROR);
            }

            return limit;
        }
    }

    @Override
    public synchronized void runTimer() throws Exception {
        tasks.defaultExecutor().start(this::collectMetrics);
    }

    private void collectMetrics() {
        final DataCollector<Metric> collector = DataCollector.create();
        for (MetricProvider provider : providers) {
            collectMetrics(collector, provider);
        }
        List<Metric> newMetricsList = collector.getData();
        Collections.sort(newMetricsList);
        metricsList = newMetricsList;
    }

    private void collectMetrics(DataCollector<Metric> collector, MetricProvider provider) {
        try {
            provider.gather(new MetricCollectorAdapter(collector));
        } catch (Exception e) {
            Exceptions.handle(e);
        }
    }

    /**
     * Returns a list of all collected metrics so far.
     *
     * @return a list of all metrics
     */
    public List<Metric> getMetrics() {
        return Collections.unmodifiableList(metricsList);
    }
}
