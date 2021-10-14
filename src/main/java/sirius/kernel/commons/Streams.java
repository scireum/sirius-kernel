/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides utility methods when working with {@link InputStream input-} and {@link OutputStream output streams}.
 */
public class Streams {

    private Streams() {

    }

    /**
     * Contains the proper character which can be written into a <tt>Writer</tt> to add BOM.
     */
    public static final int UNICODE_BOM_CHARACTER = '\ufeff';

    /**
     * Transfers all bytes from the given source to the destination.
     *
     * @param source      the source to read bytes from
     * @param destination the destination to write the bytes to
     * @return the total number of transferred bytes
     * @throws IOException in case of an IO error thrown by either the source or the destination
     */
    public static long transfer(@Nonnull InputStream source, @Nonnull OutputStream destination) throws IOException {
        byte[] buffer = new byte[8192];
        long transferredBytes = 0;
        int readBytes = 0;
        while ((readBytes = source.read(buffer)) > 0) {
            destination.write(buffer, 0, readBytes);
            transferredBytes += readBytes;
        }

        return transferredBytes;
    }

    /**
     * Reads all available bytes in the given source.
     *
     * @param source the source to read all bytes from
     * @return the number of bytes read
     * @throws IOException in case of an IO error while reading from the source
     */
    public static long exhaust(@Nonnull InputStream source) throws IOException {
        byte[] buffer = new byte[8192];
        long totalBytes = 0;
        int readBytes = 0;
        while ((readBytes = source.read(buffer)) > 0) {
            totalBytes += readBytes;
        }

        return totalBytes;
    }

    /**
     * Converts the given input stream into a <tt>byte[]</tt>.
     * <p>
     * Please be aware that this will load the entire contents of the given stream into heap memory.
     *
     * @param source the source to read from
     * @return all contents which have been read from the stream
     * @throws IOException in case of an IO error while reading
     */
    public static byte[] toByteArray(@Nonnull InputStream source) throws IOException {
        ByteArrayOutputStream destination = new ByteArrayOutputStream();
        transfer(source, destination);
        return destination.toByteArray();
    }

    /**
     * Transfers all bytes from the given source to the destination.
     *
     * @param source      the source to read bytes from
     * @param destination the destination to write the bytes to
     * @return the total number of transferred bytes
     * @throws IOException in case of an IO error thrown by either the source or the destination
     */
    public static long transfer(@Nonnull Reader source, @Nonnull Writer destination) throws IOException {
        char[] buffer = new char[4096];
        long transferredBytes = 0;
        int readBytes = 0;
        while ((readBytes = source.read(buffer)) > 0) {
            destination.write(buffer, 0, readBytes);
            transferredBytes += readBytes;
        }

        return transferredBytes;
    }

    /**
     * Reads the whole contents of the given reader into a <tt>String</tt>.
     *
     * @param reader the source to read from
     * @return all contents which have been read from the given source
     * @throws IOException in case of an IO error while reading
     */
    public static String readToString(@Nonnull Reader reader) throws IOException {
        StringWriter destination = new StringWriter();
        transfer(reader, destination);
        return destination.toString();
    }

    /**
     * Reads the contents of the given reader and returns a list of lines.
     *
     * @param reader the reader to read from
     * @return a list of lines which have been read (as determined by {@link BufferedReader#readLine()}.
     * @throws IOException in case of an IO error while reading
     */
    public static List<String> readLines(@Nonnull Reader reader) throws IOException {
        List<String> result = new ArrayList<>();
        BufferedReader bufferedReader = new BufferedReader(reader);
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            result.add(line);
        }

        return result;
    }
}
