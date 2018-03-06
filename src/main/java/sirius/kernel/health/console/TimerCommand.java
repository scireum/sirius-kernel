/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health.console;

import sirius.kernel.Sirius;
import sirius.kernel.commons.Values;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Register;
import sirius.kernel.timer.EveryDay;
import sirius.kernel.timer.Timers;

import javax.annotation.Nonnull;

/**
 * Console command which reports the last execution of the timer tasks.
 * <p>
 * It also permits to call an timer out of schedule
 */
@Register
public class TimerCommand implements Command {

    private static final String LINE_FORMAT = "%20s %-30s";

    @Part
    private Timers ts;

    @Override
    public void execute(Output output, String... params) throws Exception {
        if (params.length == 0) {
            output.line("Usage: timer all|oneMinute|tenMinutes|oneHour|everyDay <hour>");
        } else {
            if ("all".equalsIgnoreCase(params[0]) || "oneMinute".equalsIgnoreCase(params[0])) {
                output.line("Executing one minute timers...");
                ts.runOneMinuteTimers();
            }
            if ("all".equalsIgnoreCase(params[0]) || "tenMinutes".equalsIgnoreCase(params[0])) {
                output.line("Executing ten minute timers...");
                ts.runTenMinuteTimers();
            }
            if ("all".equalsIgnoreCase(params[0]) || "oneHour".equalsIgnoreCase(params[0])) {
                output.line("Executing one hour timers...");
                ts.runOneHourTimers();
            }
            if ("everyDay".equalsIgnoreCase(params[0])) {
                int currentHour = Values.of(params).at(1).asInt(25);
                output.line("Executing daily timers for hour: " + currentHour);
                ts.runEveryDayTimers(currentHour);
            }
        }

        output.blankLine();
        output.line("System Timers - Last Execution");
        output.separator();
        output.apply(LINE_FORMAT, "One-Minute", ts.getLastOneMinuteExecution());
        output.apply(LINE_FORMAT, "Ten-Minutes", ts.getLastTenMinutesExecution());
        output.apply(LINE_FORMAT, "One-Hour", ts.getLastHourExecution());
        output.separator();
        output.blankLine();
        output.line("Daily Tasks");
        output.separator();
        for (EveryDay task : ts.getDailyTasks()) {
            output.apply("%30s: %2sh",
                         task.getConfigKeyName(),
                         Sirius.getSettings().getInt(Timers.TIMER_DAILY_PREFIX + task.getConfigKeyName()));
        }
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
