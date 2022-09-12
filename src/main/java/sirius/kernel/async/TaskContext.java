/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

import sirius.kernel.commons.RateLimit;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.std.Part;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Provides an interface between a running task and a monitoring system.
 * <p>
 * Any task or background job can access its <tt>TaskContext</tt> using either {@link TaskContext#get()} or
 * {@link sirius.kernel.async.CallContext#get(Class)}. This provides an interface to a monitoring system which
 * might be present (by calling {@link sirius.kernel.async.TaskContext#setAdapter(TaskContextAdapter)}. If no
 * monitoring is available, the default mechanisms of the platform are used.
 */
public class TaskContext implements SubContext {
    /**
     * Forms the default value used to specify the system string which identifies the currently active module.
     *
     * @see #getSystemString()
     */
    private static final String GENERIC = "GENERIC";

    /**
     * One the system string is changed, it will be updated in the mapped diagnostic context (MDC) using this name.
     *
     * @see #setSystem(String)
     * @see #setSubSystem(String)
     * @see #setJob(String)
     */
    public static final String MDC_SYSTEM = "system";

    private TaskContextAdapter adapter;
    private String system = GENERIC;
    private String subSystem = GENERIC;
    private String job = GENERIC;
    private final CallContext parent;

    @Part
    private static Tasks tasks;

    /**
     * Generates a new TaskContext.
     * <p>
     * Normally this is should only be invoked by {@link CallContext}. Use {@link CallContext#get(Class)} to obtain an
     * instance.
     */
    public TaskContext() {
        this.adapter = new BasicTaskContextAdapter(this);
        this.parent = CallContext.getCurrent();
    }

    /**
     * Provides access to the <tt>TaskContext</tt> for the current thread.
     * <p>
     * This is boilerplate for {@code CallContext.getCurrent().get(TaskContext.class)}
     *
     * @return the task context for the current thread
     */
    public static TaskContext get() {
        return CallContext.getCurrent().get(TaskContext.class);
    }

    /**
     * Writes a log message to the monitor.
     * <p>
     * If no monitor is available, the <tt>async</tt> logger will be used.
     *
     * @param message the message to log
     * @param args    the parameters used to format the message (see {@link Strings#apply(String, Object...)})
     */
    public void log(String message, Object... args) {
        if (adapter != null) {
            adapter.log(Strings.apply(message, args));
        } else {
            Tasks.LOG.INFO(getSystemString() + ": " + Strings.apply(message, args));
        }
    }

    /**
     * Writes a debug message to the monitor.
     * <p>
     * If no monitor is available, the <tt>async</tt> logger will be used.
     *
     * @param message the message to log
     * @param args    the parameters used to format the message (see {@link Strings#apply(String, Object...)})
     */
    public void trace(String message, Object... args) {
        adapter.trace(Strings.apply(message, args));
    }

    /**
     * Logs the given message and sets it as current state.
     *
     * @param message the message to log
     * @param args    the parameters used to format the message (see {@link Strings#apply(String, Object...)})
     */
    public void logAsCurrentState(String message, Object... args) {
        log(message, args);
        tryUpdateState(message, args);
    }

    /**
     * Sets the new state of the current task.
     *
     * @param newState the message to set as state
     * @param args     the parameters used to format the state message (see {@link Strings#apply(String, Object...)})
     * @deprecated Use either {@link #tryUpdateState(String, Object...)} or {@link #forceUpdateState(String, Object...)}
     */
    @Deprecated(forRemoval = true)
    public void setState(String newState, Object... args) {
        adapter.setState(Strings.apply(newState, args));
    }

    /**
     * Tries to update the state message.
     * <p>
     * This will employ a rate limiting and suppress updates which are too frequent (as updating the state might
     * be quite a task in downstream frameworks like <tt>Processes</tt> of <tt>sirius-biz</tt>.
     *
     * @param newState the message to set as state
     * @param args     the parameters used to format the state message (see {@link Strings#apply(String, Object...)})
     */
    public void tryUpdateState(String newState, Object... args) {
        adapter.tryUpdateState(Strings.apply(newState, args));
    }

    /**
     * Forces the update of the state message.
     * <p>
     * This will not care about any rate limiting (unlike {@link #tryUpdateState(String, Object...)}) and always force
     * an update.
     *
     * @param newState the message to set as state
     * @param args     the parameters used to format the state message (see {@link Strings#apply(String, Object...)})
     */
    public void forceUpdateState(String newState, Object... args) {
        adapter.forceUpdateState(Strings.apply(newState, args));
    }

    /**
     * Logs the given message unless the method is called too frequently.
     * <p>
     * This method has an internal rate limit and can therefore be used by loops etc. to report the progress
     * every now and then.
     * <p>
     * A caller can rely on the rate limit and therefore can invoke this method as often as desired. However
     * one must not rely on any message to be shown.
     * <p>
     * Note that the default implementation will skip these logs entirely.
     *
     * @param message the message to add to the logs.
     */
    public void logLimited(Object message) {
        adapter.logLimited(message);
    }

    /**
     * Logs the given message unless the method is called too frequently.
     * <p>
     * Note that the given supplier is only evaluated if the message will be actually invoked and thus might save the
     * system from doing excessive computations.
     *
     * @param messageSupplier the supplier which yields the message to log on demand
     */
    public void smartLogLimited(Supplier<Object> messageSupplier) {
        adapter.smartLogLimited(messageSupplier);
    }

    /**
     * Increments the given performance counter by one and supplies a loop duration in milliseconds.
     * <p>
     * The avarage value will be computed for the given counter and gives the user a rough estimate what the current
     * task is doing.
     * <p>
     * Note that the default implementation will simply ignore the provided timings.
     *
     * @param counter the counter to increment
     * @param millis  the current duration for the block being counted
     */
    public void addTiming(String counter, long millis) {
        adapter.addTiming(counter, millis, false);
    }

    /**
     * Increments the given performance counter by one and supplies a loop duration in milliseconds.
     * <p>
     * The avarage value will be computed for the given counter and gives the user a rough estimate what the current
     * task is doing.
     * <p>
     * Note that the default implementation will simply ignore the provided timings.
     *
     * @param counter   the counter to increment
     * @param millis    the current duration for the block being counted
     * @param adminOnly whether to show the timing only to administrators instead of all users
     */
    public void addTiming(String counter, long millis, boolean adminOnly) {
        adapter.addTiming(counter, millis, adminOnly);
    }

    /**
     * Can be used to determine if the state should be refreshed.
     * <p>
     * By calling {@code shouldUpdateState().check()} an inner loop can detect if a state update should be
     * performed. This will limit the number of updates to a reasonable value.
     *
     * @return a rate limit which limits the number of updates to a reasonable value
     */
    public RateLimit shouldUpdateState() {
        return adapter.shouldUpdateState();
    }

    /**
     * Signals the monitor that the execution had an error.
     * <p>
     * Although an error is signaled, this will not cancel or interrupt the execution of the task. This is merely
     * a signal for an user or administrator that an unexpected or non-anticipated event occurred.
     */
    public void markErroneous() {
        adapter.markErroneous();
    }

    /**
     * Determines if the execution of this task is still active.
     * <p>
     * A task can be either stopped via the {@link #cancel()} method or due to a system shutdown. In any case it is
     * wise for a task to check this flag every once in a while to keep the overall app responsive.
     *
     * @return <tt>true</tt> as long as the task is expected to be executed, <tt>false</tt> otherwise
     */
    public boolean isActive() {
        return adapter.isActive() && tasks.isRunning();
    }

    /**
     * Cancels the execution of this task.
     * <p>
     * Note that this will not kill the underlying thread. This will merely toggle the canceled flag. It is
     * however the task programmers job to check this flag and interrupt / terminate all computations.
     */
    public void cancel() {
        adapter.cancel();
    }

    /**
     * Utility to iterate through a collection while checking the cancelled flag.
     *
     * @param iterable the collection to iterate through
     * @param consumer the processor invoked for each element
     * @param <T>      the type of elements being processed
     */
    public <T> void iterateWhileActive(Iterable<T> iterable, Consumer<T> consumer) {
        for (T obj : iterable) {
            if (!isActive()) {
                return;
            }
            consumer.accept(obj);
        }
    }

    /**
     * Returns the <tt>System String</tt>.
     * <p>
     * This will consist of three parts: System, Sub-System and Job. It is used to provide information which
     * module is currently active. Therefore the <tt>System</tt> will provide a raw information which module is
     * active. This might be <b>HTTP</b> for the web server or the category of an executor in {@link Tasks}.
     * <p>
     * The <tt>Sub-System</tt> will provide a more detailed information, like the class name or the name of
     * a component which is currently active.
     * <p>
     * Finally the <tt>Job</tt> will provide a detailed information what's being currently processed. This might be
     * the effective URI of the request being processed by the web server or the name of a file currently being
     * imported.
     *
     * @return the <tt>System String</tt> with a format like <i>System::Sub-System::Job</i>
     */
    public String getSystemString() {
        return system + "::" + subSystem + "::" + job;
    }

    /**
     * Returns the <tt>System</tt> component of the <tt>System String</tt>
     *
     * @return the system component of the system string
     * @see #getSystemString()
     */
    public String getSystem() {
        return system;
    }

    /**
     * Sets the <tt>System</tt> component of the <tt>System String</tt>
     *
     * @param system the new system component to set
     * @return the task context itself for fluent method calls
     * @see #getSystemString()
     */
    public TaskContext setSystem(String system) {
        if (Strings.isEmpty(system)) {
            this.system = GENERIC;
        } else {
            this.system = system;
        }
        parent.addToMDC(MDC_SYSTEM, getSystemString());
        return this;
    }

    /**
     * Returns the <tt>Sub-System</tt> component of the <tt>System String</tt>
     *
     * @return the sub system component of the system string
     * @see #getSystemString()
     */
    public String getSubSystem() {
        return subSystem;
    }

    /**
     * Sets the <tt>Sub-System</tt> component of the <tt>System String</tt>
     *
     * @param subSystem the new sub system component to set
     * @return the task context itself for fluent method calls
     * @see #getSystemString()
     */
    public TaskContext setSubSystem(String subSystem) {
        if (Strings.isEmpty(subSystem)) {
            this.subSystem = GENERIC;
        } else {
            this.subSystem = subSystem;
        }
        parent.addToMDC(MDC_SYSTEM, getSystemString());
        return this;
    }

    /**
     * Returns the <tt>Job</tt> component of the <tt>System String</tt>
     *
     * @return the job component of the system string
     * @see #getSystemString()
     */
    public String getJob() {
        return job;
    }

    /**
     * Sets the <tt>Job</tt> component of the <tt>System String</tt>
     *
     * @param job the new job component to set
     * @return the task context itself for fluent method calls
     * @see #getSystemString()
     */
    public TaskContext setJob(String job) {
        if (Strings.isEmpty(job)) {
            this.job = GENERIC;
        } else {
            this.job = job;
        }
        parent.addToMDC(MDC_SYSTEM, getSystemString());
        return this;
    }

    @Override
    public String toString() {
        return getSystemString();
    }

    /**
     * Returns the monitoring adapter which is currently active.
     *
     * @return the monitoring adapter or <tt>null</tt> if no adapter is active
     */
    public TaskContextAdapter getAdapter() {
        return adapter;
    }

    /**
     * Installs the given adapter as monitoring adapter.
     *
     * @param adapter the adapter to install
     */
    public void setAdapter(TaskContextAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public SubContext fork() {
        TaskContext child = new TaskContext();
        child.adapter = adapter;

        return child;
    }

    @Override
    public void detach() {
        // Nothing to do...
    }
}
