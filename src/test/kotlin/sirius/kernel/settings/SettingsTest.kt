/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.settings

import com.typesafe.config.ConfigFactory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import sirius.kernel.Sirius
import sirius.kernel.SiriusExtension
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests the [Settings] class.
 */
@ExtendWith(SiriusExtension::class)
class SettingsTest {

    @Test
    fun `Inner configs are delivered in their given order`() {
        val keys = Sirius.getSettings().getConfigs("test-configs").stream().map { it.getString("value") }.toList()
        assertEquals(listOf("2", "1", "3"), keys)
    }

    @Test
    fun `Inner configs are delivered as sorted by their priority if given`() {
        val keys =
                Sirius.getSettings().getConfigs("test-configs-sorted").stream().map { it.getString("value") }.toList()
        assertEquals(listOf("1", "2", "3"), keys)
    }

    @Test
    fun `No exception is thrown for retrieving a non-existent extension, even when settings are strict`() {
        val extension = Sirius.getSettings().getExtension("non-existent", "not-specified")
        assertNull(extension)
    }

    @Test
    fun `Settings#getTranslatedString works as expected`() {
        val settings = Settings(
                ConfigFactory.parseString(//language=HOCON
                        """
                            directKey = "test"
                            intKey = 5
                            translatedKey = "${'$'}testKey"
                            mapKey {
                                en: test
                                de: "ein Test"
                                default: "fallback"
                            }
                        """
                ), false
        )

        assertEquals("", settings.getTranslatedString("unknown", "en"))
        assertEquals("test", settings.getTranslatedString("directKey", "en"))
        assertEquals("testKey", settings.getTranslatedString("translatedKey", "en"))
        assertEquals("5", settings.getTranslatedString("intKey", "en"))
        assertEquals("test", settings.getTranslatedString("mapKey", "en"))
        assertEquals("ein Test", settings.getTranslatedString("mapKey", "de"))
        assertEquals("fallback", settings.getTranslatedString("mapKey", "xx"))
    }

}
