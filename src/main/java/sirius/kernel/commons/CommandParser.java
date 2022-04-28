/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import parsii.tokenizer.LookaheadReader;

import javax.annotation.Nullable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parses command invocations as expected by {@link Runtime#exec(String[])}.
 * <p>
 * A command is a verb, followed by additional parameters (0..N). Parameters are separated by one or more
 * whitespaces. If a parameter value itself contains whitespaces, it can be put into quotes:
 * <tt>command arg1 arg2 "argument 3"</tt>.
 */
public class CommandParser {

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private final String input;
    private String command;
    private List<String> args;

    /**
     * Creates a new command parser for the given input.
     *
     * @param input the command string to parse
     */
    public CommandParser(String input) {
        this.input = input;
    }

    /**
     * Extracts the command (the first token) of the given input.
     *
     * @return the actual name of the command or <tt>null</tt> if the input was empty
     */
    @Nullable
    public String parseCommand() {
        if (command == null) {
            parse();
        }

        return command;
    }

    /**
     * Obtains the list of arguments in the given string.
     *
     * @return the list of arguments in the input
     */
    public List<String> getArgs() {
        if (args == null) {
            parse();
        }

        return Collections.unmodifiableList(args);
    }

    /**
     * Returns the arguments as array.
     *
     * @return the arguments as returend by {@link #getArgs()} readily converted into an array.
     */
    public String[] getArgArray() {
        return getArgs().toArray(EMPTY_STRING_ARRAY);
    }

    private void parse() {
        command = null;
        args = new ArrayList<>();

        if (Strings.isEmpty(input)) {
            return;
        }

        LookaheadReader reader = new LookaheadReader(new StringReader(input));
        skipWhitespace(reader);
        while (!reader.current().isEndOfInput()) {
            String token = parseToken(reader);

            if (Strings.isFilled(token)) {
                if (command == null) {
                    command = token;
                } else {
                    args.add(token);
                }
            }
            skipWhitespace(reader);
        }
    }

    private void skipWhitespace(LookaheadReader reader) {
        while (reader.current().isWhitespace()) {
            reader.consume();
        }
    }

    private String parseToken(LookaheadReader reader) {
        if (reader.current().is('\"')) {
            return parseEscapedToken(reader);
        } else {
            return parseNormalToken(reader);
        }
    }

    private String parseNormalToken(LookaheadReader reader) {
        StringBuilder current = new StringBuilder();

        while (!reader.current().isEndOfInput() && !reader.current().isWhitespace()) {
            current.append(reader.consume().getValue());
        }

        return current.toString();
    }

    private String parseEscapedToken(LookaheadReader reader) {
        StringBuilder current = new StringBuilder();

        reader.consume();
        while (!reader.current().isEndOfInput() && !reader.current().is('\"')) {
            if (reader.current().is('\\')) {
                reader.consume();
                if (!reader.current().isEndOfInput()) {
                    current.append(reader.consume().getValue());
                }
            } else {
                current.append(reader.consume().getValue());
            }
        }

        return current.toString();
    }
}
