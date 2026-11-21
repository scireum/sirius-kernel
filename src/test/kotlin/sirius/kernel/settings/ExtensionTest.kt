/*
 * Made with all the love in the world
 * by scireum in Stuttgart, Germany
 *
 * Copyright by scireum GmbH
 * https://www.scireum.de - info@scireum.de
 */

package sirius.kernel.settings

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import sirius.kernel.Sirius
import sirius.kernel.SiriusExtension
import kotlin.test.assertEquals

/**
 * Tests how an [Extension] hands out values which are translation keys.
 */
@ExtendWith(SiriusExtension::class)
class ExtensionTest {

    @Test
    fun `A translation key is handed out as configured`() {
        val extension = translated()

        assertEquals(KEY, extension.get("label").asString())
        assertEquals(KEY, extension.getString("label"))
        assertEquals(KEY, extension.require("label").asString())
    }

    @Test
    fun `The context of an extension holds a translation key as configured`() {
        assertEquals(KEY, translated().context.get("label"))
    }

    @Test
    fun `A translation key is translated when asked for explicitly`() {
        val extension = translated()

        assertEquals("translation test", extension.getTranslatedString("label", "en"))
        assertEquals("übersetzungs test", extension.getTranslatedString("label", "de"))
    }

    @Test
    fun `A value which is no translation key is handed out as configured either way`() {
        val extension = translated()

        assertEquals("Who you gonna call?", extension.getString("plain"))
        assertEquals("Who you gonna call?", extension.getTranslatedString("plain", "en"))
    }

    @Test
    fun `A value starting with a dollar sign keeps it when there is no such translation`() {
        assertEquals("\$nls.test.unknown", translated().getString("unknown"))
    }

    @Test
    fun `A map of translations is translated when asked for explicitly`() {
        val extension = translated()

        assertEquals("Hello", extension.getTranslatedString("greeting", "en"))
        assertEquals("Hallo", extension.getTranslatedString("greeting", "de"))
        assertEquals("Hi", extension.getTranslatedString("greeting", "fr"))
    }

    private fun translated(): Extension = Sirius.getSettings().getExtension("test-extensions", "translated")!!

    companion object {
        private const val KEY = "\$nls.test.translate"
    }
}
