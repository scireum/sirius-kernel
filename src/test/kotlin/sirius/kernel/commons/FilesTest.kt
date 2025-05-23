/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */
package sirius.kernel.commons

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests the [Files] class.
 */
internal class FilesTest {

    @Test
    fun `getFileExtension works as expected`() {
        assertEquals("txt", Files.getFileExtension("text.txt"))
        assertEquals("txt", Files.getFileExtension("/foo/bar/text.txt"))
        assertEquals("txt", Files.getFileExtension("/foo/bar/text.foo.txt"))
        assertEquals("txt", Files.getFileExtension("/foo.bar/text.txt"))
        assertNull(Files.getFileExtension("/foo/bartext"))
        assertNull(Files.getFileExtension("/foo.foo/bartext"))
        assertNull(Files.getFileExtension(""))
        assertNull(Files.getFileExtension(null))
    }

    @Test
    fun `getBasepath works as expected`() {
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
    fun `getFilenameAndExtension works as expected`() {
        assertEquals("test.txt", Files.getFilenameAndExtension("/foo/bar/test.txt"))
        assertEquals("test.txt", Files.getFilenameAndExtension("test.txt"))
        assertEquals("test.txt", Files.getFilenameAndExtension("bar/test.txt"))
        assertEquals("test", Files.getFilenameAndExtension("bar/test"))
        assertEquals("test", Files.getFilenameAndExtension("/foo.foo/test"))
        assertNull(Files.getFilenameAndExtension("/foo/"))
        assertNull(Files.getFilenameAndExtension("/"))
        assertNull(Files.getFilenameAndExtension(""))
        assertNull(Files.getFilenameAndExtension(null))
    }

    @Test
    fun `getFilenameWithoutExtension works as expected`() {
        assertEquals("test", Files.getFilenameWithoutExtension("test.txt"))
        assertEquals("test", Files.getFilenameWithoutExtension("test"))
        assertEquals("test", Files.getFilenameWithoutExtension("/bar/test.txt"))
        assertEquals("test", Files.getFilenameWithoutExtension("/bar/test"))
        assertEquals("test", Files.getFilenameWithoutExtension("/bar.bar/test"))
        assertNull(Files.getFilenameWithoutExtension("/foo/"))
        assertNull(Files.getFilenameWithoutExtension("/"))
        assertNull(Files.getFilenameWithoutExtension(""))
        assertNull(Files.getFilenameWithoutExtension(null))
    }

    @Test
    fun `toSaneFileName works as expected`() {
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
        assertNull(Files.toSaneFileName("   ").orElse(null))
        assertNull(Files.toSaneFileName("").orElse(null))
    }

    @Test
    fun `isConsideredHidden works as expected`(){
        assertEquals(true, Files.isConsideredHidden(".test"));
        assertEquals(true, Files.isConsideredHidden(".test.tmp"));
        assertEquals(false, Files.isConsideredHidden("test"));
        assertEquals(false, Files.isConsideredHidden("test.tmp"));
        assertEquals(false,Files.isConsideredHidden(null));
    }

    @Test
    fun `isConsideredMetadata works as expected`(){
        assertEquals(true, Files.isConsideredMetadata("__MACOSX"));
        assertEquals(true, Files.isConsideredMetadata("__MACOSX/test"));
        assertEquals(true, Files.isConsideredMetadata("__MACOSX/folder1/test"));
        assertEquals(true, Files.isConsideredMetadata(".DS_Store"));
        assertEquals(true, Files.isConsideredMetadata("Thumbs.db"));
        assertEquals(false, Files.isConsideredMetadata("test.tmp"));
        assertEquals(false, Files.isConsideredMetadata("thumbs.db"));
        assertEquals(false, Files.isConsideredMetadata(null));
    }

    @Test
    fun `streamPath works as expected`() {
        assert(Files.streamPath("test1").toList().equals(listOf("test1")));
        assertEquals(listOf("test2", "test1"), Files.streamPath("/test1/test2").toList());
        assertEquals(listOf("test3", ".test2", "test1"), Files.streamPath("/test1/.test2/test3").toList());
        assertEquals(listOf("test4.png", "test3", "test2", "test1"), Files.streamPath("/test1/test2/test3/test4.png").toList());
        assertEquals(listOf(null, "test2", "test1"), Files.streamPath("/test1/test2/").toList());
    }
}
