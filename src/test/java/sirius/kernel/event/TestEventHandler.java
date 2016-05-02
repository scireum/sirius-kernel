/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.event;

import sirius.kernel.di.std.Register;

@Register(classes = EventHandler.class)
public class TestEventHandler implements EventHandler<String>{

    public static String testString = "beforeTest";

    @Override
    public String getEvent() {
        return "String-call";
    }

    @Override
    public void handle(String event, String object) {
        testString = object;
    }
}
