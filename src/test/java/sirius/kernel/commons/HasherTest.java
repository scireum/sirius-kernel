/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HasherTest {

    @Test
    void testMD5() throws URISyntaxException, IOException {
        assertEquals("b10a8db164e0754105b7a99be72e3fe5", Hasher.md5().hash("Hello World").toHexString());
        assertEquals("sQqNsWTgdUEFt6mb5y4/5Q==", Hasher.md5().hash("Hello World").toBase64String());
        assertEquals("e59ff97941044f85df5297e1c302d260",
                     Hasher.md5()
                           .hashFile(new File(getClass().getResource("/hash_test_file.txt").toURI()))
                           .toHexString());
    }

    @Test
    void testSHA() {
        assertEquals("0a4d55a8d778e5022fab701977c5d840bbc486d0", Hasher.sha1().hash("Hello World").toHexString());
        assertEquals("a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e",
                     Hasher.sha256().hash("Hello World").toHexString());
        assertEquals(
                "2c74fd17edafd80e8447b0d46741ee243b7eb74dd2149a0ab1b9246fb30382f27e853d8585719e0e67cbda0daa8f51671064615d645ae27acb15bfb1447f459b",
                Hasher.sha512().hash("Hello World").toHexString());
    }
}
