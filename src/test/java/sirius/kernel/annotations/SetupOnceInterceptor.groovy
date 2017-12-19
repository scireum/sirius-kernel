/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.annotations

import org.spockframework.runtime.extension.AbstractMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation

class SetupOnceInterceptor extends AbstractMethodInterceptor {

    String methodToInvoke

    @Override
    void interceptFeatureExecution(IMethodInvocation invocation) throws Throwable {

        //Get the instance of the executed spec
        def currentlyRunningSpec = invocation.sharedInstance

        //Execute the setup method we specified in the annotation value
        currentlyRunningSpec."$methodToInvoke"()
        invocation.proceed()
    }
}
