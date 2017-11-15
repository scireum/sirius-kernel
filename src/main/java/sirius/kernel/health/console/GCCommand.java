/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health.console;

import sirius.kernel.commons.Explain;
import sirius.kernel.di.std.Register;
import sirius.kernel.nls.NLS;

import javax.annotation.Nonnull;

/**
 * Console command to invoke the garbage collector.
 * <p>
 * This command will also report the heap consumption before and after the call.
 */
@Register
public class GCCommand implements Command {

    private static final String LINE_FORMAT = "%-20s %10s";

    @Override
    @SuppressWarnings( "squid:S1215")
    @Explain("The whole purpose of this call is to invoke the garbage collector.")
    public void execute(Output output, String... params) throws Exception {
        output.apply(LINE_FORMAT, "TYPE", "SIZE");
        output.separator();
        output.apply(LINE_FORMAT, "Free", NLS.formatSize(Runtime.getRuntime().freeMemory()));
        output.apply(LINE_FORMAT, "Total", NLS.formatSize(Runtime.getRuntime().totalMemory()));
        output.apply(LINE_FORMAT, "Max", NLS.formatSize(Runtime.getRuntime().maxMemory()));
        Runtime.getRuntime().gc();
        output.separator();
        output.apply(LINE_FORMAT, "Free", NLS.formatSize(Runtime.getRuntime().freeMemory()));
        output.apply(LINE_FORMAT, "Total", NLS.formatSize(Runtime.getRuntime().totalMemory()));
        output.apply(LINE_FORMAT, "Max", NLS.formatSize(Runtime.getRuntime().maxMemory()));
        output.separator();
    }

    @Override
    @Nonnull
    public String getName() {
        return "gc";
    }

    @Override
    public String getDescription() {
        return "Invokes the garbage collector of the JVM";
    }
}
