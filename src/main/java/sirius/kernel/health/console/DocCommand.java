/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health.console;

import sirius.kernel.commons.Value;
import sirius.kernel.di.Injector;
import sirius.kernel.di.std.Register;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;

/**
 * Provides a simple API documentation by accepting a class name as parameter and listing all methods as result.
 * <p>
 * This command will try to find a class matching the given name and output all methods accessible methods along
 * with their signature.
 */
@Register
public class DocCommand implements Command {

    @Override
    public void execute(Output output, String... params) throws Exception {
        Value name = Value.indexOf(0, params);
        if (name.isEmptyString()) {
            output.line("No class name given. Try: doc <classname>");
            return;
        }
        Injector.getAllLoadedClasses()
                .stream()
                .filter(c -> c.getName().toLowerCase().contains(name.asString().toLowerCase()))
                .forEach(c -> {
                    output.line(c.getName());
                    output.separator();
                    for (Method m : c.getMethods()) {
                        output.line(m.toString());
                    }
                    output.blankLine();
                });
    }

    @Override
    @Nonnull
    public String getName() {
        return "doc";
    }

    @Override
    public String getDescription() {
        return "API-Doc utility. Provides a list of methods for a given class name.";
    }
}
