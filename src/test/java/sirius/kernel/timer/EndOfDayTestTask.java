/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.timer;

import sirius.kernel.async.Future;
import sirius.kernel.di.std.Register;

@Register
public class EndOfDayTestTask implements EndOfDayTask {

    protected static Future executed = new Future();

    @Override
    public String getName() {
        return "test";
    }

    @Override
    public void execute() throws Exception {
        executed.success();
    }

}
