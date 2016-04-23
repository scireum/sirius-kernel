/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health.console;

import org.apache.log4j.Level;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Log;

import javax.annotation.Nonnull;

/**
 * Permits to change the level of a logger at runtime.
 */
@Register
public class LoggerCommand implements Command {

    @Override
    public void execute(Output output, String... params) {
        if (params.length == 2) {
            output.apply("Setting %s to: %s", params[0], params[1]);
            Log.setLevel(params[0], Level.toLevel(params[1]));
            output.blankLine();
        } else {
            output.line("Usage: logger <name> <LEVEL>");
            output.blankLine();
            output.line("Known loggers:");
            output.separator();
            for (Log l : Log.getAllLoggers()) {
                output.line(l.getName());
            }
            output.separator();
        }
    }

    @Override
    @Nonnull
    public String getName() {
        return "logger";
    }

    @Override
    public String getDescription() {
        return "Changes the level of a logger at runtime.";
    }
}
