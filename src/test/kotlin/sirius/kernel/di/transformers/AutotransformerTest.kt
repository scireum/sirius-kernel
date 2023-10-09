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

@ExtendWith(SiriusExtension::class)
class AutotransformerSpec {

    @Test
    fun `Autotransforming into a subclass of TargetClassAutotransform works directly`() {
        val parent = ParentClass()
        assertTrue {
            parent.tryAs(TargetClassAutotransformChild::class.java).isPresent
        }
        assertInstanceOf(
                TargetClassAutotransformChild::class.java,
                parent.`as`(TargetClassAutotransformChild::class.java)
        )
    }

    @Test
    fun `Autotransforming into a subclass of TargetClassAutotransform must not instantiate twice`() {
        val parent = ParentClass()
        assertTrue {
            parent.tryAs(TargetClassAutotransformChild::class.java).isPresent
        }
        assertInstanceOf(
                TargetClassAutotransformChild::class.java,
                parent.`as`(TargetClassAutotransformChild::class.java)
        )
        assertTrue {
            parent.tryAs(TargetClassAutotransform::class.java).isPresent
        }
        assertInstanceOf(
                TargetClassAutotransform::class.java,
                parent.`as`(TargetClassAutotransform::class.java)
        )
    }

    @Test
    fun `Autotransforming mixture of targets and target`() {
        val parent = ParentClass()
        assertTrue {
            parent.tryAs(TargetClassAutotransformChildWeird::class.java).isPresent
        }
        assertInstanceOf(
                TargetClassAutotransformChildWeird::class.java,
                parent.`as`(TargetClassAutotransformChildWeird::class.java)
        )
        assertTrue {
            parent.tryAs(TargetClassAutotransform::class.java).isPresent
        }
        assertInstanceOf(
                TargetClassAutotransform::class.java,
                parent.`as`(TargetClassAutotransform::class.java)
        )
    }

}
