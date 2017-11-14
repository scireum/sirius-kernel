/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health.console;

import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Register;
import sirius.kernel.timer.Timers;

import javax.annotation.Nonnull;

/**
 * Console command which reports the last execution of the timer tasks.
 * <p>
 * It also permits to call an timer out of schedule
 */
@Register
public class TimerCommand implements Command {

    @Part
    private Timers ts;

    @Override
    @SuppressWarnings("squid:S1192")
    public void execute(Output output, String... params) throws Exception {
        if (params.length == 0) {
            output.line("Usage: timer all|oneMinute|tenMinutes|oneHour|everyDay");
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
            if ("all".equalsIgnoreCase(params[0]) || "everyDay".equalsIgnoreCase(params[0])) {
                output.line("Executing daily timers...");
                ts.runEveryDayTimers(true);
            }
        }
        output.blankLine();
        output.line("System Timers - Last Execution");
        output.separator();
        output.apply("%20s %-30s", "One-Minute", ts.getLastOneMinuteExecution());
        output.apply("%20s %-30s", "Ten-Minutes", ts.getLastTenMinutesExecution());
        output.apply("%20s %-30s", "One-Hour", ts.getLastHourExecution());
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
