/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import sirius.kernel.async.TaskContext;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Provides a simple reader which parses given CSV (comma separated values) data into rows.
 * <p>
 * By default <tt>;</tt> is used to separate columns and a line break (either Windows or Unix) is used
 * to separate rows. Also columns can be enclosed in quotations, especially if line breaks occur within
 * a value. The default character used to signal quaotation is <tt>&quot;</tt>. Note that the quotation symbol has to
 * be
 * the first non-whitespace character in the column to be detected as such (or the very first character if
 * {@link #notIgnoringWhitespaces()} was called during initialisation).
 * <p>
 * Furthermore escaping can be used to embed a column separator or a quotation character in a column value.
 * By default <tt>\</tt> is used as escape character.
 * <p>
 * Empty columns will be represented as empty strings. Values will not be trimmed, as this can be easily achieved
 * using the {@link Values} which is used to represent a parsed row.
 * <p>
 * An example use case would be:
 * {@code new CSVReader(someInput).execute(row -&gt; doSomethingSmartPerRow(row)); }
 * <p>
 * Note that this class checks the {@link TaskContext} during execution. Therefore if the underlying task is cancelled,
 * the parser will stop after the current row has been processed.
 */
public class CSVReader {

    private final Reader input;
    private char separator = ';';
    private char quotation = '"';
    private boolean ignoreWhitespaces = true;
    private char escape = '\\';
    private Consumer<Values> consumer;
    private int buffer;
    private Limit limit = Limit.UNLIMITED;

    /**
     * Creates a new reader which processes the given input.
     * <p>
     * Note that the given input is consumed character by character so using a {@link java.io.BufferedReader}
     * might be a good idea as most devices rather exchange larger blocks of data (e.g. 8kb).
     * <p>
     * If {@link #execute(Consumer)} is invoked, the given input will be closed once all data has been parsed or if and
     * IO error occurs.
     *
     * @param input the input to parse
     */
    public CSVReader(@Nonnull Reader input) {
        this.input = input;
    }

    /**
     * Specifies the separator character to use.
     * <p>
     * By default this is <tt>;</tt>.
     *
     * @param separator the separator to use
     * @return the reader itself for fluent method calls
     */
    public CSVReader withSeparator(char separator) {
        this.separator = separator;
        return this;
    }

    /**
     * Specifies the quotation character to use.
     * <p>
     * By default this is <tt>"</tt>. Use <tt>\0</tt> to disable quotation entirely.
     *
     * @param quotation the quotation character to use
     * @return the reader itself for fluent method calls
     */
    public CSVReader withQuotation(char quotation) {
        this.quotation = quotation;
        return this;
    }

    /**
     * Specifies the escape character to use.
     * <p>
     * By default this is <tt>\</tt>. Use <tt>\0</tt> to disable escaping entirely.
     *
     * @param escape the escape character to use
     * @return the reader itself for fluent method calls
     */
    public CSVReader withEscape(char escape) {
        this.escape = escape;
        return this;
    }

    /**
     * Disables the flexible whitespace behaviour.
     * <p>
     * If a column starts with whitespaces (space or tab characters) and is then quoted, the whitespaces
     * around the quotes are simply ignored. Therefore <tt>;"a";</tt>, <tt>; "a" ;</tt> and <tt>;a;</tt> will
     * yield the same result. However <tt>; a ;</tt> will keep the whitespaces and has to be manually trimmed.
     * <p>
     * Calling this method will disable this behavior and <tt>;  "a"  ;</tt> will yield <tt> "a" </tt>
     * as column value instead of <tt>a</tt>.
     *
     * @return the reader itself for fluent method calls
     */
    public CSVReader notIgnoringWhitespaces() {
        this.ignoreWhitespaces = false;
        return this;
    }

    /**
     * Can set a limit to read only a specific range of rows from the file.
     * <p>
     * By default all rows are read.
     *
     * @param limit the limit to use reading the rows from the file.
     * @return the reader itself for fluent method calls
     */
    public CSVReader withLimit(Limit limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Parses the previously supplied input and calls the given consumer for each row.
     * <p>
     * Note that this method will close the given input.
     *
     * @param consumer the consume to call for each line
     * @throws IOException if an IO error occures while reading from the given input
     */
    public void execute(Consumer<Values> consumer) throws IOException {
        try {
            this.consumer = consumer;
            TaskContext tc = TaskContext.get();
            read();
            while (tc.isActive() && !isEOF() && limit.shouldContinue()) {
                readRow();
                consumeNewLine();
            }
        } finally {
            input.close();
        }
    }

    /*
     * Consumes a windows or unix style line break.
     */
    private void consumeNewLine() throws IOException {
        if (buffer == '\r') {
            read();
        }
        if (buffer == '\n') {
            read();
        }
    }

    /*
     * Fills the internal buffer by reading from the stream.
     */
    private void read() throws IOException {
        buffer = input.read();
    }

    /*
     * Reads a single row from the stream. This might be multiple lines from the
     * input as quotet columns may contain line breaks.
     */
    private void readRow() throws IOException {
        List<String> row = new ArrayList<>();
        while (!isEOF() && !isAtNewline()) {
            row.add(readField());
            if (buffer == separator) {
                read();
            }
        }

        if (limit.nextRow()) {
            consumer.accept(Values.of(row));
        }
    }

    /*
     * Reads a single column.
     */
    private String readField() throws IOException {
        StringBuilder result = new StringBuilder();
        boolean inQuote = false;
        skipLeadingWhitespaces(result);

        if (buffer == quotation) {
            inQuote = true;
            read();
            if (ignoreWhitespaces) {
                result = new StringBuilder();
            }
        }

        readFieldValue(result, inQuote);
        skipTrailingWhitespaces(inQuote);

        return result.toString();
    }

    private void skipLeadingWhitespaces(StringBuilder result) throws IOException {
        if (ignoreWhitespaces) {
            while (buffer == ' ' || buffer == '\t') {
                result.append((char) buffer);
                read();
            }
        }
    }

    private void skipTrailingWhitespaces(boolean inQuote) throws IOException {
        if (inQuote) {
            while (buffer == ' ' || buffer == '\t') {
                read();
            }
        }
    }

    private void readFieldValue(StringBuilder result, boolean inQuote) throws IOException {
        while (shouldContinueField(inQuote)) {
            if (buffer == escape) {
                read();
                if (!isEOF()) {
                    result.append((char) buffer);
                }
            } else {
                result.append((char) buffer);
            }
            read();
        }
    }

    /*
     * Determines if the current buffer value should be added to the field (column) content.
     */
    private boolean shouldContinueField(boolean inQuote) throws IOException {
        if (isEOF()) {
            return false;
        }

        if (inQuote) {
            if (buffer == quotation) {
                read();
                return buffer == quotation;
            }
            return buffer != quotation;
        } else {
            return buffer != separator && !isAtNewline();
        }
    }

    /*
     * Determines if the current buffer indicates a line break.
     */
    private boolean isAtNewline() {
        return buffer == '\r' || buffer == '\n';
    }

    /*
     * Determines if we reached the end of the steam / reader.
     */
    public boolean isEOF() {
        return buffer == -1;
    }
}
