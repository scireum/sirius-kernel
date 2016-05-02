/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.event;

/**
 * Interface of EventHandler handling an event
 * The implementing class needs to be registered
 * as {@link EventHandler} in order to be
 * recognized by the {@link EventBus}
 *
 * @param <E> type of class handled by the implementing handler
 */
public interface EventHandler<E> {

    /**
     * Name of event the handler is listening on
     * {@link EventBus}
     *
     * @return name of event
     */
    String getEvent();

    /**
     * Method handling a matching event
     *
     * @param event  name of the matching event
     * @param object fired with the event
     */
    void handle(String event, E object);
}
