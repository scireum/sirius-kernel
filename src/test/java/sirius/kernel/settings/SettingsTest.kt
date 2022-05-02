/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.settings

import com.typesafe.config.ConfigFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SettingsTest {

    @Test
    fun `Settings#getTranslatedString works as expected`() {
        val settings = Settings(
                ConfigFactory.parseString(
                        """
            directKey = "test"
            intKey = 5
            translatedKey = "${'$'}testKey"
            mapKey {
                en: test
            }
            
            mapKeyWithDefault {
                default: test
            }            
        """
                ), true
        )

        Assertions.assertEquals("", settings.getTranslatedString("unknown", "en"))
        Assertions.assertEquals("test", settings.getTranslatedString("directKey", "en"))
        Assertions.assertEquals("testKey", settings.getTranslatedString("translatedKey", "en"))
        Assertions.assertEquals("5", settings.getTranslatedString("intKey", "en"))
        Assertions.assertEquals("test", settings.getTranslatedString("mapKey", "en"))
        Assertions.assertEquals("test", settings.getTranslatedString("mapKeyWithDefault", "en"))
    }

}
