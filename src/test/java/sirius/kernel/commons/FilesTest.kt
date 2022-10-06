/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */
package sirius.kernel.commons

import org.junit.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FilesTest {
    @Test
    fun testgetFileExtension() {
        assertEquals("txt", Files.getFileExtension("text.txt"))
        assertEquals("txt", Files.getFileExtension("/foo/bar/text.txt"))
        assertEquals("txt", Files.getFileExtension("/foo/bar/text.foo.txt"))
        assertNull(Files.getFileExtension("/foo/bartext"))
        assertNull(Files.getFileExtension(""))
        assertNull(Files.getFileExtension(null))
    }

    @Test
    fun testgetBasepath() {
        assertEquals("/foo", Files.getBasepath("/foo/test.txt"))
        assertEquals("/foo", Files.getBasepath("/foo/bar"))
        assertEquals("/foo/bar", Files.getBasepath("/foo/bar/test.txt"))
        assertEquals("/foo/bar", Files.getBasepath("/foo/bar/"))
        assertNull(Files.getBasepath("/foo"))
        assertNull(Files.getBasepath("/"))
        assertNull(Files.getBasepath("test.txt"))
        assertNull(Files.getBasepath(""))
        assertNull(Files.getBasepath(null))
    }

    @Test
    fun testgetFilenameAndExtension() {
        assertEquals("test.txt", Files.getFilenameAndExtension("/foo/bar/test.txt"))
        assertEquals("test.txt", Files.getFilenameAndExtension("test.txt"))
        assertEquals("test.txt", Files.getFilenameAndExtension("bar/test.txt"))
        assertEquals("test", Files.getFilenameAndExtension("bar/test"))
        assertNull(Files.getFilenameAndExtension("/foo/"))
        assertNull(Files.getFilenameAndExtension("/"))
        assertNull(Files.getFilenameAndExtension(""))
        assertNull(Files.getFilenameAndExtension(null))
    }

    @Test
    fun testgetFilenameWithoutExtension() {
        assertEquals("test", Files.getFilenameWithoutExtension("test.txt"))
        assertEquals("test", Files.getFilenameWithoutExtension("test"))
        assertEquals("test", Files.getFilenameWithoutExtension("/bar/test.txt"))
        assertEquals("test", Files.getFilenameWithoutExtension("/bar/test"))
        assertNull(Files.getFilenameWithoutExtension("/foo/"))
        assertNull(Files.getFilenameWithoutExtension("/"))
        assertNull(Files.getFilenameWithoutExtension(""))
        assertNull(Files.getFilenameWithoutExtension(null))
    }

    @Test
    fun toSaneFilename() {
        assertEquals("test.pdf", Files.toSaneFileName("test.pdf").orElse(""))
        assertEquals("test", Files.toSaneFileName("test").orElse(""))
        assertEquals(".pdf", Files.toSaneFileName(".pdf").orElse(""))
        assertEquals("test.", Files.toSaneFileName("test.").orElse(""))
        assertEquals("test_.", Files.toSaneFileName("test..").orElse(""))
        assertEquals("_.test", Files.toSaneFileName("..test").orElse(""))
        assertEquals("Test_pdf", Files.toSaneFileName("Test pdf").orElse(""))
        assertEquals("Halloele", Files.toSaneFileName("Hallöle").orElse(""))
        assertEquals("test_datei", Files.toSaneFileName("test/datei").orElse(""))
        assertEquals("test-datei", Files.toSaneFileName("test-datei").orElse(""))
        assertEquals("test", Files.toSaneFileName(" test ").orElse(""))
        assertEquals("test_datei.pdf", Files.toSaneFileName("test.datei.pdf").orElse(""))
        assertEquals(Optional.empty(), Files.toSaneFileName("   "))
        assertEquals(Optional.empty(), Files.toSaneFileName(""))
    }
}
