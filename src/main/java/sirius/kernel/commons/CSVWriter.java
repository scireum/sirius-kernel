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
 * If the quotation character occurs within an already quoted string, it is escaped using <tt>\</tt>. If no
 * quotation character is specified (set to <tt>\0</tt>), the escape character is used if possible. If quoting or
 * escaping is required but disabled (using <tt>\0</tt> for their respective value), an exception will be thrown as no
 * valid output can be generated.
 */
public class CSVWriter implements Closeable {

    private String lineSeparator = "\n";
    private final Writer writer;
    private boolean firstLine = true;
    private char separator = ';';
    private String separatorString = String.valueOf(';');
    private char quotation = '"';
    private boolean isQuotationEmpty = false;
    private boolean forceQuotation = false;
    private char escape = '\\';
    private boolean isEscapeEmpty = false;
    private boolean trim = true;

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
     * Emits a UNICODE byte order mark.
     * <p>
     * This must be invoked right after the writer has been created before any headers or rows have been written.
     *
     * @return the writer itself for fluent method calls
     * @throws IOException in case an IO error occurs while writing the BOM
     */
    public CSVWriter writeUnicodeBOM() throws IOException {
        this.writer.write(Streams.UNICODE_BOM_CHARACTER);

        return this;
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
        this.separatorString = String.valueOf(separator);
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
        this.isQuotationEmpty = quotation == '\0';
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
        this.isEscapeEmpty = escape == '\0';
        return this;
    }

    /**
     * Controls if each added cell value of the type String should be trimmed or not
     * <p>
     * By default this is <tt>true</tt>. Use <tt>false</tt> if strings should not be trimmed.
     *
     * @param trim the value controlling if strings should be trimmed
     * @return the writer itself for fluent method calls
     */
    public CSVWriter withInputTrimming(boolean trim) {
        this.trim = trim;
        return this;
    }

    /**
     * Specifies the lineSeparator used to create new lines.
     * <p>
     * By default this is <tt>\n</tt>.
     *
     * @param lineSeparator the lineSeparator to use
     * @return the writer itself for fluent method calls
     */
    public CSVWriter withLineSeparator(String lineSeparator) {
        this.lineSeparator = lineSeparator;
        return this;
    }

    /**
     * Specifies wether or not all fields in the generated CSV should be enclosed with the specified quotation character.
     * <p>
     * By default this is <tt>false</tt>, which means only fields that require quotation because they contain
     * the separator character or a line break are enclosed with quotations.
     *
     * @param force if all fields should be quoted regardless of content or not
     * @return the writer itself for fluent method calls
     */
    public CSVWriter withForceQuotation(boolean force) {
        this.forceQuotation = force;
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

    /**
     * Effectively outputs the given object as next column value.
     *
     * @param object the value to output
     * @throws IOException in case of an IO error while writing
     */
    private void writeColumn(Object object) throws IOException {
        String stringValue = convertToString(object);

        if (shouldQuote(stringValue)) {
            writer.append(quotation);
            writer.append(escapeValue(stringValue, true));
            writer.append(quotation);
        } else {
            writer.append(escapeValue(stringValue, false));
        }
    }

    /**
     * Determines if the given value needs to be quoted.
     *
     * @param stringValue the value to check
     * @return <tt>true</tt> if the value should be output as a quoted string, <tt>false</tt> otherwise
     */
    private boolean shouldQuote(String stringValue) {
        if (isQuotationEmpty) {
            return false;
        }
        if (forceQuotation) {
            return true;
        }
        return stringValue.contains(separatorString) || stringValue.contains("\n") || stringValue.contains("\r");
    }

    /**
     * Transforms the value into a string representation suitable for outputting in a CSV file.
     *
     * @param object the object to convert
     * @return a machine-readable string representation
     */
    private String convertToString(Object object) {
        if (object == null) {
            return "";
        }

        if (object instanceof String) {
            if (trim) {
                return ((String) object).trim();
            } else {
                return (String) object;
            }
        }

        return NLS.toMachineString(object);
    }

    /**
     * Escapes the given string if needed.
     *
     * @param stringValue the string to escape
     * @param shouldQuote a flag which determines if the resulting string will be quoted or not
     * @return the properly escaped string
     */
    private String escapeValue(String stringValue, boolean shouldQuote) {
        StringBuilder effectiveValue = new StringBuilder();
        for (int i = 0; i < stringValue.length(); i++) {
            char currentChar = stringValue.charAt(i);
            processCharacter(currentChar, effectiveValue, shouldQuote);
        }

        return effectiveValue.toString();
    }

    private void processCharacter(char currentChar, StringBuilder effectiveValue, boolean shouldQuote) {
        if (currentChar == escape) {
            effectiveValue.append(escape).append(currentChar);
            return;
        }

        if (isQuotationEmpty) {
            processCharacterWithoutQuotation(currentChar, effectiveValue);
            return;
        }

        if (shouldQuote && currentChar == quotation) {
            if (isEscapeEmpty) {
                throw new IllegalArgumentException(
                        "Cannot output a quotation character within a quoted string without an escape character.");
            } else {
                effectiveValue.append(escape);
            }
        }
        effectiveValue.append(currentChar);
    }

    private void processCharacterWithoutQuotation(char currentChar, StringBuilder effectiveValue) {
        if (currentChar == separator) {
            if (isEscapeEmpty) {
                throw new IllegalArgumentException(Strings.apply(
                        "Cannot output a column which contains the separator character '%s' "
                        + "without an escape or quotation character.",
                        separator));
            } else {
                effectiveValue.append(escape).append(currentChar);
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
