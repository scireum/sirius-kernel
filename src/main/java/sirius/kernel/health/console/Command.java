/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health.console;

import sirius.kernel.di.std.Named;

import java.io.PrintWriter;

/**
 * Describes a command which is callable via the system console (http://localhost:9000/system/console).
 */
public interface Command extends Named {

    /**
     * Encapsulates the output functionality used by commands to render their output.
     */
    interface Output {
        /**
         * Provides access to the underlying {@link PrintWriter}
         *
         * @return the underlying writer
         */
        PrintWriter getWriter();

        /**
         * Shortcut to print a new line.
         *
         * @return the {@link Output} itself for fluent method calls.
         */
        Output blankLine();

        /**
         * Shortcut to print the given contents in a single line.
         *
         * @param contents to contents to print
         * @return the {@link Output} itself for fluent method calls.
         */
        Output line(String contents);

        /**
         * Shortcut to print a line filled with "-------------"
         *
         * @return the {@link Output} itself for fluent method calls.
         */
        Output separator();

        /**
         * Formats the given string by replacing all parameters with the given columns.
         * <p>
         * This is a shortcut for {@code line(String.format(format, columns))}.
         *
         * @param format  the format used to output the data.
         * @param columns the parameters supplied to the formatter
         * @return the {@link Output} itself for fluent method calls.
         * @see String#format(String, Object...)
         */
        Output apply(String format, Object... columns);
    }

    /**
     * Executes the given command with the given parameters.
     *
     * @param output provides access to the output interface used to generate output
     * @param params provides the parameters entered in the console
     * @throws Exception in case of an error. Throw a {@link sirius.kernel.health.HandledException} to
     *                             signal, that all logging and handling has already been performed.
     *                             Any other exception will be logged and reported as system error.
     */
    void execute(Output output, String... params) throws Exception;

    /**
     * Returns a short description of the command.
     *
     * @return the description of the command
     */
    String getDescription();
}
