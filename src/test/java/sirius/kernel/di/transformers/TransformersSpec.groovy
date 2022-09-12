/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.transformers

import sirius.kernel.BaseSpecification

class TransformersSpec extends BaseSpecification {

    def "Transforming two classes regularly"() {
        given:
        def parent = new ParentClass()
        expect:
        parent.tryAs(TargetClass.class).isPresent()
        parent.as(TargetClass.class) instanceof TargetClass
    }

    def "Transforming first child class"() {
        given:
        def firstChild = new FirstChildClass()
        expect:
        firstChild.tryAs(TargetClass.class).isPresent()
        firstChild.as(TargetClass.class) instanceof TargetClass
    }

    def "Transforming second child class"() {
        given:
        def secondChild = new SecondChildClass()
        expect:
        secondChild.tryAs(TargetClass.class).isPresent()
        secondChild.as(TargetClass.class) instanceof TargetClass
    }
}
