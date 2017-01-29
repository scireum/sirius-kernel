/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health.console;

import sirius.kernel.di.GlobalContext;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Register;
import sirius.kernel.info.Product;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Console command which generates a help screen listing all commands.
 */
@Register
public class HelpCommand implements Command {

    @Part
    private GlobalContext ctx;

    @Override
    public void execute(Output output, String... params) throws Exception {
        output.blankLine();
        output.apply("C O N S O L E  -  %s", Product.getProduct());
        output.blankLine();
        output.apply("%-20s %s", "CMD", "DESCRIPTION");
        output.separator();
        List<Command> parts = new ArrayList<>(ctx.getParts(Command.class));
        parts.sort(Comparator.comparing(Command::getName));
        for (Command cmd : parts) {
            output.apply("%-20s %s", cmd.getName(), cmd.getDescription());
        }
        output.separator();
    }

    @Override
    @Nonnull
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Generates this help screen.";
    }
}
