/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.transformers

class AutotransformerSpec extends BaseSpecification {

    def "Autotransforming into a subclass of TargetClassAutotransform works directly"() {
        given:
        def parent = new ParentClass()
        expect:
        parent.tryAs(TargetClassAutotransformChild.class).isPresent()
                parent.as(TargetClassAutotransformChild.class) instanceof TargetClassAutotransformChild
    }

    def "Autotransforming into a subclass of TargetClassAutotransform must not instantiate twice"() {
        given:
        def parent = new ParentClass()
        expect:
        parent.tryAs(TargetClassAutotransformChild.class).isPresent()
                parent.as(TargetClassAutotransformChild.class) instanceof TargetClassAutotransformChild
                parent.tryAs(TargetClassAutotransform.class).isPresent()
                        parent.as(TargetClassAutotransform.class) instanceof TargetClassAutotransform
    }

    def "Autotransforming mixture of targets and target"() {
        given:
        def parent = new ParentClass()
        expect:
        parent.tryAs(TargetClassAutotransformChildWeird.class).isPresent()
                parent.as(TargetClassAutotransformChildWeird.class) instanceof TargetClassAutotransformChildWeird
                parent.tryAs(TargetClassAutotransform.class).isPresent()
                        parent.as(TargetClassAutotransform.class) instanceof TargetClassAutotransform
    }

}
