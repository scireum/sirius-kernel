/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

/**
 * Default implementation for <tt>TaskContextAdapter</tt>
 */
public class BasicTaskContextAdapter implements TaskContextAdapter {

    protected volatile boolean cancelled = false;
    protected volatile boolean erroneous = false;
    protected String state;
    protected TaskContext ctx;

    /**
     * Creates a new <tt>BasicTaskContextAdapter</tt> for the given <tt>TaskContext</tt>.
     *
     * @param ctx the current task context for which this adapter is created
     */
    public BasicTaskContextAdapter(TaskContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void log(String message) {
        Async.LOG.INFO(ctx.getSystemString() + ": " + message);
    }

    @Override
    public void trace(String message) {
        if (Async.LOG.isFINE()) {
            Async.LOG.FINE(ctx.getSystemString() + ": " + message);
        }
    }

    @Override
    public void setState(String message) {
        this.state = message;
    }

    @Override
    public void inc(String counter, long duration) {
        // IGNORED
    }

    @Override
    public void markErroneous() {
        erroneous = true;
    }

    @Override
    public void cancel() {
        cancelled = true;
    }

    @Override
    public void setJobTitle(String jobTitle) {
        // IGNORED
    }

    @Override
    public boolean isActive() {
        return !cancelled;
    }
}
