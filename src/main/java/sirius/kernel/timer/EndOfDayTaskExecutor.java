/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.timer;

import sirius.kernel.async.Tasks;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.TimeProvider;
import sirius.kernel.commons.Watch;
import sirius.kernel.di.PartCollection;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Parts;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Executes all {@link EndOfDayTask end of day tasks} in the given timeframe.
 *
 * @see EndOfDayTask for a detailed description of the theory of operations
 */
@Register(classes = {EndOfDayTaskExecutor.class, EveryDay.class})
public class EndOfDayTaskExecutor implements EveryDay {

    @ConfigValue("timer.daily.end-of-day")
    private int startHour;

    @ConfigValue("health.end-of-day-limit")
    private int endHour;

    @Part
    private Tasks tasks;

    @Part
    private TimeProvider timeProvider;

    @Parts(EndOfDayTask.class)
    private PartCollection<EndOfDayTask> endOfDayTasks;

    private final Map<EndOfDayTask, EndOfDayTaskInfo> taskInfos = new ConcurrentHashMap<>();

    @Override
    public String getConfigKeyName() {
        return "end-of-day";
    }

    @Override
    public void runTimer() throws Exception {
        tasks.defaultExecutor().start(this::executeTasks);
    }

    private void executeTasks() {
        // As we limit the execution to a certain time frame we shuffle the execution on each run
        // so that no single task can steal all the processing time...
        List<EndOfDayTask> tasksToExecute = new ArrayList<>(endOfDayTasks.getParts());
        Collections.shuffle(tasksToExecute);

        for (EndOfDayTask task : tasksToExecute) {
            if (!inValidTimeWindow()) {
                Log.BACKGROUND.INFO("Aborting end of day tasks, as the time limit has been hit...");
                return;
            }

            Log.BACKGROUND.INFO("Executing end of day task: %s", task.getName());
            EndOfDayTaskInfo info = executeTask(task);
            Log.BACKGROUND.INFO("Executed %s - Took: %s, Success: %s, Last Error: %s",
                                task.getName(),
                                info.getFormattedLastDuration(),
                                info.isLastExecutionWasSuccessful(),
                                info.getLastErrorMessage());
        }
    }

    /**
     * Determines if processing should keep running.
     *
     * @return a condition which yields <tt>true</tt> as soon as processing should be aborted
     */
    private boolean inValidTimeWindow() {
        int currentHour = timeProvider.localTimeNow().getHour();

        if (startHour < endHour) {
            // If the start hour is before the end hour, we can simply check for the exact time slice...
            return currentHour >= startHour && currentHour <= endHour;
        } else {
            // otherwise we check from start to midnight and from midnight to end...
            return currentHour >= startHour || currentHour <= endHour;
        }
    }

    private EndOfDayTaskInfo executeTask(EndOfDayTask task) {
        EndOfDayTaskInfo info = getInfo(task);
        Watch w = Watch.start();
        try {
            info.lastExecution = LocalDateTime.now();
            task.execute();
            info.lastExecutionWasSuccessful = true;
            info.lastErrorMessage = null;
        } catch (Exception e) {
            info.lastExecutionWasSuccessful = false;
            info.lastErrorMessage = e.getMessage() + " (" + e.getClass().getName() + ")";
            Exceptions.handle()
                      .to(Log.BACKGROUND)
                      .error(e)
                      .withSystemErrorMessage("An error occured when executing end of day task %s (%s): %s (%s)",
                                              task.getName(),
                                              task.getClass())
                      .handle();
        } finally {
            info.lastDuration = w.elapsedMillis();
        }

        return info;
    }

    private EndOfDayTaskInfo getInfo(EndOfDayTask task) {
        return taskInfos.computeIfAbsent(task, this::createInfo);
    }

    private EndOfDayTaskInfo createInfo(EndOfDayTask endOfDayTask) {
        EndOfDayTaskInfo info = new EndOfDayTaskInfo();
        info.task = endOfDayTask;

        return info;
    }

    /**
     * Lists the recorded execution infos for all end of day tasks.
     *
     * @return a list of execution infos for all end of day tasks
     */
    public List<EndOfDayTaskInfo> getTaskInfos() {
        return endOfDayTasks.getParts()
                            .stream()
                            .map(this::getInfo)
                            .sorted(Comparator.comparing(info -> info.getTask().getName()))
                            .collect(Collectors.toList());
    }

    /**
     * Executes a task out of schedule (in a blocking manner).
     *
     * @param name the name of the task to execute
     * @return the execution info which has been recorded
     */
    public Optional<EndOfDayTaskInfo> executeNow(String name) {
        for (EndOfDayTask task : endOfDayTasks) {
            if (Strings.areEqual(name, task.getName())) {
                return Optional.of(executeTask(task));
            }
        }

        return Optional.empty();
    }
}
