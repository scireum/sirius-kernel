/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigBuilderTest {

    @Test
    fun `Config builder works`() {
        val configBuilder = ConfigBuilder()
        configBuilder.addVariable("scope.foo", "true")
        assertEquals(//language=HOCON
                "scope.foo = true",
                configBuilder.toString()
        )
    }

    @Test
    fun `Scope unfolding works`() {
        val configBuilder = ConfigBuilder()
        configBuilder.addVariable("scope.foo", "true")
        configBuilder.addVariable("scope.bar", "false")
        assertEquals(//language=HOCON
                """
                    scope {
                        foo = true
                        bar = false
                    }
                """.trimIndent(),
                configBuilder.toString()
        )
    }

    @Test
    fun `Complex example works`() {
        val configBuilder = ConfigBuilder()
        configBuilder.addVariable("scopeA.foo", "true")
        configBuilder.addVariable("scopeA.bar", "true")
        configBuilder.addVariable("scopeB.foo", "\"enabled\"")
        configBuilder.addVariable("scopeB.scopeC.foo", "\"disabled\"")
        configBuilder.addVariable("scopeB.scopeC.bar", "\"enabled\"")
        configBuilder.addVariable("foo", "\"disabled\"")
        configBuilder.addVariable("foo.bar.test", "42")

        assertEquals(//language=HOCON
                """
                    foo = "disabled"
                    foo.bar.test = 42
                    
                    scopeA {
                        foo = true
                        bar = true
                    }
                    
                    scopeB {
                        foo = "enabled"
                    
                        scopeC {
                            foo = "disabled"
                            bar = "enabled"
                        }
                    }
                """.trimIndent(),
                configBuilder.toString()
        )
    }

}
