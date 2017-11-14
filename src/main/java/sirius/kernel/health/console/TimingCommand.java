/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health.console;

import sirius.kernel.commons.MultiMap;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Average;
import sirius.kernel.health.Microtiming;

import javax.annotation.Nonnull;

/**
 * Console command which enables/disables the all mighty Micro-Timing framework.
 * <p>
 * It also reports timings recorded for the last period of time.
 */
@Register
public class TimingCommand implements Command {

    @Override
    public void execute(Output output, String... params) throws Exception {
        if (params.length == 1 && Strings.isFilled(params[0])) {
            parseParameters(output, params[0]);
        } else {
            if (Microtiming.isEnabled()) {
                generateOutput(output);
            } else {
                output.line("Microtiming is disabled! Use: 'timing +' to enable.");
            }
        }
    }

    public void parseParameters(Output output, String param) {
        if ("enable".equalsIgnoreCase(param) || "+".equalsIgnoreCase(param)) {
            enableTiming(output);
        } else if ("disable".equalsIgnoreCase(param) || "-".equalsIgnoreCase(param)) {
            disableTiming(output);
        } else {
            output.line("Usage: timing enable|disable (You can use + and - for enable/disable).");
            output.line("To enable tracing: timing trace <filter-expression>");
        }
    }

    public void disableTiming(Output output) {
        generateOutput(output);
        Microtiming.setEnabled(false);
        output.line("Disabling Microtiming...");
    }

    public void enableTiming(Output output) {
        if (Microtiming.isEnabled()) {
            generateOutput(output);
            Microtiming.setEnabled(false);
            output.line("Resetting Microtiming...");
        } else {
            output.line("Enabling Microtiming...");
        }
        Microtiming.setEnabled(true);
    }

    /**
     * Generates the output for all recorded micro timings.
     *
     * @param output the output interface to send the output to
     */
    protected void generateOutput(Output output) {
        long delta = System.currentTimeMillis() - Microtiming.getLastReset();
        Microtiming.getTimings()
                   .stream()
                   .collect(MultiMap.groupingBy(MultiMap::create, Microtiming.Timing::getCategory))
                   .stream()
                   .forEach(c -> {
                       output.line(c.getKey());
                       output.separator();
                       output.apply("%8s %9s %5s %5s %s", "AVG[ms]", "TOTAL[ms]", "RATIO", "COUNT", "NAME");
                       output.separator();
                       c.getValue().forEach(v -> {
                           Average avg = v.getAvg();
                           double totalTime = avg.getAvg() / 1000d * avg.getCount();
                           double percentTime = (totalTime * 100d) / delta;
                           output.apply("%8.2f %9d %4.2f%% %5d %s",
                                        avg.getAvg() / 1000d,
                                        Math.round(totalTime),
                                        percentTime,
                                        avg.getCount(),
                                        v.getKey());
                       });
                       output.separator();
                       output.blankLine();
                   });
    }

    @Override
    @Nonnull
    public String getName() {
        return "timing";
    }

    @Override
    public String getDescription() {
        return "Reports statistics recorded by the micro timer";
    }
}
