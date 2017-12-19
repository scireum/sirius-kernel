/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.annotations

import org.spockframework.runtime.extension.AbstractAnnotationDrivenExtension
import org.spockframework.runtime.model.FeatureInfo

class SetupOnceExtension extends AbstractAnnotationDrivenExtension<SetupOnce> {

    @Override
    void visitFeatureAnnotation(SetupOnce annotation, FeatureInfo feature) {

        //Retrieve the name of the setup method we'd like to invoke from our annotation
        def methodToInvoke = annotation.value()

        //Construct and subscribe our event interceptor
        def interceptor = new SetupOnceInterceptor(methodToInvoke: methodToInvoke)
        feature.addInterceptor(interceptor)
    }
}
