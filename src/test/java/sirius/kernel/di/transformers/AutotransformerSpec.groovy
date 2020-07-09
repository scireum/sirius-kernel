/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.transformers

import sirius.kernel.BaseSpecification

class AutotransformerSpec extends BaseSpecification {

    def "Autotransforming into a subclass of TargetClassAutotransform"() {
        given:
        def parent = new ParentClass()
        expect:
        parent.tryAs(TargetClassAutotransformChild.class).isPresent()
        parent.as(TargetClassAutotransformChild.class) instanceof TargetClassAutotransformChild
    }

}
