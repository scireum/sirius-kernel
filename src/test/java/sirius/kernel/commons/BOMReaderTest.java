/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

class BOMReaderTest {

    private static final byte[] WITH_UTF8_BOM = {(byte) 239, (byte) 187, (byte) 191, 'H', 'E', 'L', 'L', 'O'};

    @Test
    void readBOM() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        writer.write(Streams.UNICODE_BOM_CHARACTER);
        writer.write("HELLO");
        writer.flush();
        BOMReader in = new BOMReader(new InputStreamReader(new ByteArrayInputStream(outputStream.toByteArray())));
        Assertions.assertEquals('H', in.read());
        Assertions.assertEquals('E', in.read());
    }

    @Test
    void readWithoutBOM() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        writer.write("HELLO");
        writer.flush();
        BOMReader in = new BOMReader(new InputStreamReader(new ByteArrayInputStream(outputStream.toByteArray())));
        Assertions.assertEquals('H', in.read());
        Assertions.assertEquals('E', in.read());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 5})
    void readArrayBOM(int length) throws IOException {
        BOMReader in = new BOMReader(new InputStreamReader(new ByteArrayInputStream(WITH_UTF8_BOM)));
        char[] buf = new char[length];
        Assertions.assertEquals(length, in.read(buf));
        Assertions.assertEquals('H', buf[0]);
    }

    @Test
    void readArrayWithoutBOM() throws IOException {
        BOMReader in = new BOMReader(new InputStreamReader(new ByteArrayInputStream(WITH_UTF8_BOM)));
        char[] buf = new char[2];
        Assertions.assertEquals(2, in.read(buf));
        Assertions.assertEquals('H', buf[0]);
    }
}
