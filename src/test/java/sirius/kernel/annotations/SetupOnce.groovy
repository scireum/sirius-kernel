/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.annotations

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)

//Specify the extension class that backs this annotation
@org.spockframework.runtime.extension.ExtensionAnnotation(SetupOnceExtension)

@interface SetupOnce {

    //Accept a string value that represents the name of the setup method to execute
    String value()
}
