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
import sirius.kernel.nls.NLS;
import sirius.kernel.timer.EndOfDayTaskExecutor;
import sirius.kernel.timer.EndOfDayTaskInfo;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Console command which reports all {@link sirius.kernel.timer.EndOfDayTask end of day tasks}.
 * <p>
 * It also permits to call a task out of schedule.
 */
@Register
public class EndOfDayCommand implements Command {

    @Part
    private EndOfDayTaskExecutor endOfDayTaskExecutor;

    @Override
    public void execute(Output output, String... params) throws Exception {
        if (params.length == 1) {
            executeSingleTask(output, params[0]);
        } else {
            reportAllTasks(output);
        }
    }

    private void reportAllTasks(Output output) {
        output.line("Usage: eod <task-name>");
        output.blankLine();
        output.line("End of Day Tasks");
        output.separator();
        output.apply("%30s %20s: %-30s", "NAME", "EXECUTED", "DURATION");
        output.separator();
        for (EndOfDayTaskInfo info : endOfDayTaskExecutor.getTaskInfos()) {
            output.apply("%30s %20s: %-30s",
                         info.getTask().getName(),
                         NLS.toUserString(info.getLastExecution()),
                         info.getFormattedLastDurartion());
            output.line(info.getLastErrorMessage());
            output.blankLine();
        }
        output.separator();
    }

    private void executeSingleTask(Output output, String taskName) {
        output.apply("Executing task: %s", taskName);
        Optional<EndOfDayTaskInfo> taskInfo = endOfDayTaskExecutor.executeNow(taskName);
        if (taskInfo.isPresent()) {
            output.apply("Executed %s - Took: %s, Success: %s, Last Error: %s",
                         taskInfo.get().getTask().getName(),
                         taskInfo.get().getFormattedLastDurartion(),
                         taskInfo.get().isLastExecutionWasSuccessful(),
                         taskInfo.get().getLastErrorMessage());
        } else {
            output.apply("Unknown task: %s", taskName);
        }
    }

    @Override
    @Nonnull
    public String getName() {
        return "eod";
    }

    @Override
    public String getDescription() {
        return "Reports the last execution of all known end of day tasks. Also a specific task can be forced to run.";
    }
}
