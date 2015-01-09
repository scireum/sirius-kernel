/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.junit.Assert.*;

public class BOMReaderTest {

    private byte[] WITH_UTF8_BOM = new byte[]{(byte) 239, (byte) 187, (byte) 191, 'H', 'E', 'L', 'L', 'O'};
    private byte[] WITHOUT_BOM = new byte[]{'H', 'E', 'L', 'L', 'O'};

    @Test
    public void readBOM() throws IOException {
        BOMReader in = new BOMReader(new InputStreamReader(new ByteArrayInputStream(WITH_UTF8_BOM)));
        assertEquals('H', in.read());
        assertEquals('E', in.read());
    }

    @Test
    public void readWithoutBOM() throws IOException {
        BOMReader in = new BOMReader(new InputStreamReader(new ByteArrayInputStream(WITHOUT_BOM)));
        assertEquals('H', in.read());
        assertEquals('E', in.read());
    }

    @Test
    public void readArray0BOM() throws IOException {
        BOMReader in = new BOMReader(new InputStreamReader(new ByteArrayInputStream(WITH_UTF8_BOM)));
        char[] buf = new char[0];
        assertEquals(0, in.read(buf));
    }

    @Test
    public void readArray1BOM() throws IOException {
        BOMReader in = new BOMReader(new InputStreamReader(new ByteArrayInputStream(WITH_UTF8_BOM)));
        char[] buf = new char[1];
        assertEquals(1, in.read(buf));
        assertEquals('H', buf[0]);
    }

    @Test
    public void readArray2BOM() throws IOException {
        BOMReader in = new BOMReader(new InputStreamReader(new ByteArrayInputStream(WITH_UTF8_BOM)));
        char[] buf = new char[2];
        assertEquals(2, in.read(buf));
        assertEquals('H', buf[0]);
    }

    @Test
    public void readArray10BOM() throws IOException {
        BOMReader in = new BOMReader(new InputStreamReader(new ByteArrayInputStream(WITH_UTF8_BOM)));
        char[] buf = new char[10];
        assertEquals(5, in.read(buf));
        assertEquals('H', buf[0]);
    }

    @Test
    public void readArrayWithoutBOM() throws IOException {
        BOMReader in = new BOMReader(new InputStreamReader(new ByteArrayInputStream(WITH_UTF8_BOM)));
        char[] buf = new char[2];
        assertEquals(2, in.read(buf));
        assertEquals('H', buf[0]);
    }

}
