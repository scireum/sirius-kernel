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
import java.util.Comparator;
import java.util.Set;

/**
 * Console command which reports the last execution of the timer tasks.
 * <p>
 * It also permits to call a timer out of schedule.
 */
@Register
public class TimerCommand implements Command {

    private static final String LINE_FORMAT = "%20s %-30s";

    private static final Set<String> ACCEPTED_PARAMS = Set.of("all", "oneMinute", "tenMinutes", "oneHour", "everyDay");

    private static final String USAGE = "Usage: timer all|oneMinute|tenMinutes|oneHour|everyDay <hour>";

    @Part
    private Timers timers;

    @Override
    public void execute(Output output, String... parameters) throws Exception {
        if (parameters.length == 0) {
            output.line(USAGE);
        } else if (!ACCEPTED_PARAMS.contains(parameters[0])) {
            output.apply("'%s' is not an accepted parameter!", parameters[0]);
            output.line(USAGE);
        } else {
            if ("all".equalsIgnoreCase(parameters[0]) || "oneMinute".equalsIgnoreCase(parameters[0])) {
                output.line("Executing one minute timers...");
                timers.runOneMinuteTimers();
            }
            if ("all".equalsIgnoreCase(parameters[0]) || "tenMinutes".equalsIgnoreCase(parameters[0])) {
                output.line("Executing ten minute timers...");
                timers.runTenMinuteTimers();
            }
            if ("all".equalsIgnoreCase(parameters[0]) || "oneHour".equalsIgnoreCase(parameters[0])) {
                output.line("Executing one hour timers...");
                timers.runOneHourTimers();
            }
            if ("everyDay".equalsIgnoreCase(parameters[0])) {
                int currentHour = Values.of(parameters).at(1).asInt(25);
                output.line("Executing daily timers for hour: " + currentHour);
                timers.runEveryDayTimers(currentHour);
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
}
