/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.transformers

import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import sirius.kernel.SiriusExtension
import kotlin.test.assertTrue

/**
 * Tests the [Transformer] class.
 */
@ExtendWith(SiriusExtension::class)
class TransformerTest {

    @Test
    fun `Transforming two classes regularly`() {
        val parent = ParentClass()
        assertTrue {
            parent.tryAs(TargetClass::class.java).isPresent
        }
        assertInstanceOf(
                TargetClass::class.java,
                parent.`as`(TargetClass::class.java)
        )
    }

    @Test
    fun `Transforming first child class`() {
        val firstChild = FirstChildClass()
        assertTrue {
            firstChild.tryAs(TargetClass::class.java).isPresent
        }
        assertInstanceOf(
                TargetClass::class.java,
                firstChild.`as`(TargetClass::class.java)
        )
    }

    @Test
    fun `Transforming second child class`() {
        val secondChild = SecondChildClass()
        assertTrue {
            secondChild.tryAs(TargetClass::class.java).isPresent
        }
        assertInstanceOf(
                TargetClass::class.java,
                secondChild.`as`(TargetClass::class.java)
        )
    }
}
