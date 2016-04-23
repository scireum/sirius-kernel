/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health.console;

import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Values;
import sirius.kernel.di.std.Register;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Reports all known environment variables.
 */
@Register
public class EnvCommand implements Command {

    @Override
    public void execute(Output output, String... params) throws Exception {
        String filter = Values.of(params).at(0).toLowerCase();
        output.apply("%-39s %40s", "NAME", "VALUE");
        output.separator();
        for (Map.Entry<Object, Object> prop : System.getProperties().entrySet()) {
            if (Strings.isEmpty(filter) || prop.getKey().toString().toLowerCase().contains(filter) || prop.getValue()
                                                                                                          .toString()
                                                                                                          .toLowerCase()
                                                                                                          .contains(
                                                                                                                  filter)) {
                output.apply("%-39s %40s", prop.getKey(), prop.getValue());
            }
        }
        output.separator();
    }

    @Override
    @Nonnull
    public String getName() {
        return "env";
    }

    @Override
    public String getDescription() {
        return "Outputs the system environment.";
    }
}
