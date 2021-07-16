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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class StreamsTest {

    @Test
    public void transferTest() throws IOException {
        String testString = "Hello from the other side...";

        ByteArrayInputStream in = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Streams.transfer(in, out);

        assertEquals(testString, new String(out.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void largeTransferTest() throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("Hello World".repeat(10_000));

        String testString = builder.toString();

        ByteArrayInputStream in = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Streams.transfer(in, out);

        assertEquals(testString, new String(out.toByteArray(), StandardCharsets.UTF_8));
    }
}
