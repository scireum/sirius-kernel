/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health.console;

import sirius.kernel.Sirius;
import sirius.kernel.commons.Tuple;
import sirius.kernel.commons.Values;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Register;
import sirius.kernel.timer.Timers;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Console command which reports the last execution of the timer tasks.
 * <p>
 * It also permits to call a timer out of schedule.
 */
@Register
public class TimerCommand implements Command {

    private static final String LINE_FORMAT = "%20s %-30s";

    // matching is intentionally case-insensitive, and we compare lower-case values
    private static final Set<String> ACCEPTED_PARAMS =
            Stream.of("all", "oneMinute", "tenMinutes", "oneHour", "everyDay")
                  .map(String::toLowerCase)
                  .collect(Collectors.toSet());

    private static final String FORCE_PARAM_LONG = "--force";
    private static final String FORCE_PARAM_SHORT = "-f";

    private static final String USAGE = "Usage: timer [-f] all|oneMinute|tenMinutes|oneHour|everyDay <hour>";

    @Part
    private Timers timers;

    @Override
    public void execute(Output output, String... parameters) throws Exception {
        List<String> parameterList = new ArrayList<>(List.of(parameters));
        boolean forced = extractForceParameter(parameterList);
        String scope = parameterList.isEmpty() ? "" : parameterList.get(0);

        if (parameterList.isEmpty()) {
            output.line(USAGE);
        } else if (!ACCEPTED_PARAMS.contains(scope.toLowerCase())) {
            output.apply("'%s' is not an accepted parameter!", scope);
            output.line(USAGE);
        } else {
            if ("all".equalsIgnoreCase(scope) || "oneMinute".equalsIgnoreCase(scope)) {
                output.line("Executing one minute timers...");
                timers.runOneMinuteTimers();
            }
            if ("all".equalsIgnoreCase(scope) || "tenMinutes".equalsIgnoreCase(scope)) {
                output.line("Executing ten minute timers...");
                timers.runTenMinuteTimers();
            }
            if ("all".equalsIgnoreCase(scope) || "oneHour".equalsIgnoreCase(scope)) {
                output.line("Executing one hour timers...");
                timers.runOneHourTimers();
            }
            if ("everyDay".equalsIgnoreCase(scope)) {
                int currentHour = Values.of(parameterList).at(1).asInt(25);
                String forcedMessage = forced ? " (forced)" : "";
                output.line("Executing daily timers for hour: " + currentHour + forcedMessage);
                timers.runEveryDayTimers(currentHour, forced);
            }
        }

        output.blankLine();
        output.line("System Timers - Last Execution");
        output.separator();
        output.apply(LINE_FORMAT, "One-Minute", timers.getLastOneMinuteExecution());
        output.apply(LINE_FORMAT, "Ten-Minutes", timers.getLastTenMinutesExecution());
        output.apply(LINE_FORMAT, "One-Hour", timers.getLastHourExecution());
        output.separator();
        output.blankLine();
        output.line("Daily Tasks");
        output.separator();

        timers.getDailyTasks()
              .stream()
              .map(task -> Tuple.create(Sirius.getSettings()
                                              .getInt(Timers.TIMER_DAILY_PREFIX + task.getConfigKeyName()),
                                        task.getConfigKeyName()))
              .sorted(Comparator.comparingInt(Tuple::getFirst))
              .forEach(hourAndTask -> output.apply("%2sh: %s", hourAndTask.getFirst(), hourAndTask.getSecond()));

        output.separator();
    }

    @Override
    @Nonnull
    public String getName() {
        return "timer";
    }

    @Override
    public String getDescription() {
        return "Reports the last timer runs and executes them out of schedule.";
    }

    private boolean extractForceParameter(List<String> parameters) {
        if (parameters.contains(FORCE_PARAM_LONG)) {
            parameters.remove(FORCE_PARAM_LONG);
            return true;
        }

        if (parameters.contains(FORCE_PARAM_SHORT)) {
            parameters.remove(FORCE_PARAM_SHORT);
            return true;
        }

        return false;
    }
}
