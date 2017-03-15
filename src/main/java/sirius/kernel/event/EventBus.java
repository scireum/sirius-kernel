/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.event;

import sirius.kernel.commons.Strings;
import sirius.kernel.di.PartCollection;
import sirius.kernel.di.std.Parts;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Exceptions;

/**
 * Event bus handling fired events.
 * <p>
 * Checks every fired event against all registered {@link EventHandler}s and triggers the handle function in the
 * matching handlers.
 */

@Register(classes = EventBus.class)
public class EventBus {

    @Parts(EventHandler.class)
    private PartCollection<EventHandler<? super Object>> handlers;

    /**
     * Forwards the given object to the matching handler
     *
     * @param event  name of the event the {@link EventHandler} is listening on
     * @param object passed to the matching {@link EventHandler}
     */
    public void fireEvent(String event, Object object) {
        if (Strings.isEmpty(event)) {
            return;
        }
        for (EventHandler<? super Object> handler : handlers.getParts()) {
            if (Strings.areEqual(event, handler.getEvent())) {
                try {
                    handler.handle(event, object);
                } catch (Exception e) {
                    Exceptions.handle()
                              .withSystemErrorMessage("Error while handling event '%s': %s (%s)", event)
                              .error(e)
                              .handle();
                }
            }
        }
    }
}
