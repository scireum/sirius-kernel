/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

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
     * Transfers all bytes from the given source to the destination.
     *
     * @param source      the source to read bytes from
     * @param destination the destination to write the bytes to
     * @return the total number of transferred bytes
     * @throws IOException in case of an IO error thrown by either the source or the destination
     */
    public static long transfer(InputStream source, OutputStream destination) throws IOException {
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
    public static long exhaust(InputStream source) throws IOException {
        byte[] buffer = new byte[8192];
        long totalBytes = 0;
        int readBytes = 0;
        while ((readBytes = source.read(buffer)) > 0) {
            totalBytes += readBytes;
        }

        return totalBytes;
    }

    public static byte[] toByteArray(InputStream source) throws IOException {
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
    public static long transfer(Reader source, Writer destination) throws IOException {
        char[] buffer = new char[4096];
        long transferredBytes = 0;
        int readBytes = 0;
        while ((readBytes = source.read(buffer)) > 0) {
            destination.write(buffer, 0, readBytes);
            transferredBytes += readBytes;
        }

        return transferredBytes;
    }

    public static String readToString(Reader reader) throws IOException {
        StringWriter destination = new StringWriter();
        transfer(reader, destination);
        return destination.toString();
    }

    public static List<String> readLines(Reader reader) throws IOException {
        List<String> result = new ArrayList<>();
        BufferedReader bufferedReader = new BufferedReader(reader);
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            result.add(line);
        }

        return result;
    }
}
