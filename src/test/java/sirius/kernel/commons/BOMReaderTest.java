/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.junit.Assert.assertEquals;

class BOMReaderTest {

    private static final byte[] WITH_UTF8_BOM = {(byte) 239, (byte) 187, (byte) 191, 'H', 'E', 'L', 'L', 'O'};
    private static final byte[] WITHOUT_BOM = {'H', 'E', 'L', 'L', 'O'};

    @Test
    void readBOM() throws IOException {
        BOMReader in = new BOMReader(new InputStreamReader(new ByteArrayInputStream(WITH_UTF8_BOM)));
        assertEquals('H', in.read());
        assertEquals('E', in.read());
    }

    @Test
    void readWithoutBOM() throws IOException {
        BOMReader in = new BOMReader(new InputStreamReader(new ByteArrayInputStream(WITHOUT_BOM)));
        assertEquals('H', in.read());
        assertEquals('E', in.read());
    }

    @Test
    void readArray1BOM() throws IOException {
        BOMReader in = new BOMReader(new InputStreamReader(new ByteArrayInputStream(WITH_UTF8_BOM)));
        char[] buf = new char[1];
        assertEquals(1, in.read(buf));
        assertEquals('H', buf[0]);
    }

    @Test
    void readArray2BOM() throws IOException {
        BOMReader in = new BOMReader(new InputStreamReader(new ByteArrayInputStream(WITH_UTF8_BOM)));
        char[] buf = new char[2];
        assertEquals(2, in.read(buf));
        assertEquals('H', buf[0]);
    }

    @Test
    void readArray10BOM() throws IOException {
        BOMReader in = new BOMReader(new InputStreamReader(new ByteArrayInputStream(WITH_UTF8_BOM)));
        char[] buf = new char[10];
        assertEquals(5, in.read(buf));
        assertEquals('H', buf[0]);
    }

    @Test
    void readArrayWithoutBOM() throws IOException {
        BOMReader in = new BOMReader(new InputStreamReader(new ByteArrayInputStream(WITH_UTF8_BOM)));
        char[] buf = new char[2];
        assertEquals(2, in.read(buf));
        assertEquals('H', buf[0]);
    }
}
