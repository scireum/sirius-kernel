/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health;

import sirius.kernel.commons.Strings;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.di.std.Register;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Provides a in-memory store for logs and exceptions.
 * <p>
 * This will be inherently limited in size but should always contain the most recent logs and errors.
 */
@Register(classes = {MemoryBasedHealthMonitor.class, LogTap.class, ExceptionHandler.class})
public class MemoryBasedHealthMonitor implements ExceptionHandler, LogTap {

    protected final List<Incident> incidents = Collections.synchronizedList(new ArrayList<Incident>());
    protected final List<LogMessage> messages = Collections.synchronizedList(new ArrayList<LogMessage>());

    @ConfigValue("health.memory.max-errors")
    private int maxErrors;

    @ConfigValue("health.memory.max-logs")
    private int maxMsg;

    private final Counter numIncidents = new Counter();
    private final Counter numUniqueIncidents = new Counter();
    private final Counter numLogMessages = new Counter();

    @Override
    public void handle(Incident incident) throws Exception {
        synchronized (incidents) {
            boolean unique = true;
            Iterator<Incident> iter = incidents.iterator();
            while (iter.hasNext()) {
                if (Strings.areEqual(iter.next().getLocation(), incident.getLocation())) {
                    iter.remove();
                    unique = false;
                }
            }
            incidents.addFirst(incident);
            numIncidents.inc();
            if (unique) {
                numUniqueIncidents.inc();
            }
            while (incidents.size() > maxErrors) {
                incidents.removeLast();
            }
        }
    }

    @Override
    public void handleLogMessage(LogMessage msg) {
        synchronized (messages) {
            messages.addFirst(msg);
            numLogMessages.inc();
            while (messages.size() > maxMsg) {
                messages.removeLast();
            }
        }
    }

    /**
     * Contains all recorded incidents.
     *
     * @return all recorded incidents
     */
    public List<Incident> getIncidents() {
        return Collections.unmodifiableList(incidents);
    }

    /**
     * Contains all recorded log messages.
     *
     * @return all recorded messages
     */
    public List<LogMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    /**
     * Returns the total number of log messages encountered.
     *
     * @return the total number of log messages so far.
     */
    public long getNumLogMessages() {
        return numLogMessages.getCount();
    }

    /**
     * Returns the total number of incidents (exceptions) encountered.
     *
     * @return the total number of incidents so far.
     */
    public long getNumIncidents() {
        return numIncidents.getCount();
    }

    /**
     * Returns the total number of unique incidents (with different locations) encountered.
     *
     * @return the total number of unique incidents so far.
     */
    public long getNumUniqueIncidents() {
        return numUniqueIncidents.getCount();
    }
}
