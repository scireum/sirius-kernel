/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import sirius.kernel.nls.NLS;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Writes rows of data as CSV (comma separated values) files.
 * <p>
 * By default <tt>;</tt> is used to separate columns and line breaks are used to separate rows. If a column value
 * contains the separator character or a line break, it is quoted using <tt>&quot;</tt>.
 * <p>
 * If the quotation character occurs withing an already quoted string, it is escaped using <tt>\</tt>. If no
 * quotation charater is specified (set to <tt>\0</tt>), the escape character is used if possible. If quoting or
 * escaping
 * is required but disabled (using <tt>\0</tt> for their respective value), an exception will be thrown as no
 * valid output can be generated.
 */
public class CSVWriter implements Closeable {

    private String lineSeparator = "\n";
    private Writer writer;
    private boolean firstLine = true;
    private char separator = ';';
    private char quotation = '"';
    private char escape = '\\';

    /**
     * Creates a new writer sending data to the given writer.
     * <p>
     * The writer is closed by calling {@link #close()}.
     *
     * @param writer the target to write data to
     */
    public CSVWriter(Writer writer) {
        this.writer = writer;
    }

    /**
     * Specifies the separator character to use.
     * <p>
     * By default this is <tt>;</tt>.
     *
     * @param separator the separator to use
     * @return the writer itself for fluent method calls
     */
    public CSVWriter withSeparator(char separator) {
        this.separator = separator;
        return this;
    }

    /**
     * Specifies the quotation character to use.
     * <p>
     * By default this is <tt>"</tt>. Use <tt>\0</tt> to disable quotation entirely. Note that quotation is required
     * if columns contain the separator character or a line break.
     *
     * @param quotation the quotation character to use
     * @return the writer itself for fluent method calls
     */
    public CSVWriter withQuotation(char quotation) {
        this.quotation = quotation;
        return this;
    }

    /**
     * Specifies the escape character to use.
     * <p>
     * By default this is <tt>\</tt>. Use <tt>\0</tt> to disable escaping entirely. Note that escaping is required if
     * columns contain a quotation character inside an already quoted column. Or if values contain the separator
     * character and no quotation is possible (quotation character is \0).
     *
     * @param escape the escape character to use
     * @return the writer itself for fluent method calls
     */
    public CSVWriter withEscape(char escape) {
        this.escape = escape;
        return this;
    }

    /**
     * Writes the given list of values as row.
     *
     * @param row the data to write. <tt>null</tt> values will be completely skipped.
     * @return the writer itself for fluent method calls
     * @throws IOException in case of an IO error when writing to the underlying writer
     */
    public CSVWriter writeList(List<Object> row) throws IOException {
        if (row != null) {
            writeLineSeparator();
            for (int i = 0; i < row.size(); i++) {
                if (i > 0) {
                    writer.write(separator);
                }

                writeColumn(row.get(i));
            }
        }
        return this;
    }

    /**
     * Writes the given array of values as row.
     *
     * @param row the data to write
     * @return the writer itself for fluent method calls
     * @throws IOException in case of an IO error when writing to the underlying writer
     */
    public CSVWriter writeArray(Object... row) throws IOException {
        writeLineSeparator();
        for (int i = 0; i < row.length; i++) {
            if (i > 0) {
                writer.write(separator);
            }

            writeColumn(row[i]);
        }

        return this;
    }

    private void writeLineSeparator() throws IOException {
        if (!firstLine) {
            writer.write(lineSeparator);
        } else {
            firstLine = false;
        }
    }

    private void writeColumn(Object o) throws IOException {
        String stringValue = NLS.toMachineString(o);
        if (stringValue == null) {
            stringValue = "";
        }
        StringBuilder effectiveValue = new StringBuilder();
        boolean shouldQuote = false;
        for (int i = 0; i < stringValue.length(); i++) {
            char currentChar = stringValue.charAt(i);
            shouldQuote = processCharacter(currentChar, effectiveValue, shouldQuote);
        }
        if (shouldQuote) {
            writer.append(quotation);
            writer.append(effectiveValue);
            writer.append(quotation);
        } else {
            writer.append(effectiveValue);
        }
    }

    private boolean processCharacter(char currentChar, StringBuilder effectiveValue, boolean shouldQuote) {
        if (currentChar == escape) {
            effectiveValue.append(escape).append(currentChar);
            return shouldQuote;
        }

        if (quotation == '\0') {
            processCharacterWithoutQuotation(currentChar, effectiveValue);
            return shouldQuote;
        }

        if (currentChar == separator || currentChar == '\r' || currentChar == '\n') {
            shouldQuote = true;
        }

        if (shouldQuote && currentChar == quotation) {
            if (escape == '\0') {
                throw new IllegalArgumentException(
                        "Cannot output a quotation character within a quoted string without an escape character.");
            } else {
                effectiveValue.append(escape);
            }
        }
        effectiveValue.append(currentChar);
        return shouldQuote;
    }

    private void processCharacterWithoutQuotation(char currentChar, StringBuilder effectiveValue) {
        if (currentChar == separator) {
            if (escape != '\0') {
                effectiveValue.append(escape).append(currentChar);
            } else {
                throw new IllegalArgumentException(Strings.apply(
                        "Cannot output a column which contains the separator character '%s' "
                        + "without an escape or quotation character.",
                        separator));
            }
        } else if (currentChar == '\r' || currentChar == '\n') {
            throw new IllegalArgumentException(
                    "Cannot output a column which contains a line break without an quotation character.");
        } else {
            effectiveValue.append(currentChar);
        }
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
