/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.std;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class or interface as auto-detected by {@link Register}.
 * <p>
 * The register annotation tries to determine which classes and interfaces should be passed into the
 * {@link sirius.kernel.di.MutableGlobalContext#registerPart(Object, Class[])} call for a newly instantiated
 * part. All classes and interfaces which wear an {@link AutoRegister} annotation are picked up no matter if
 * these are the class itself, an implemented interface or one higher up in the inheritance hierarchy.
 * <p>
 * Using this mechanism, the {@link Register#classes()} parameter should be left empty in almost all cases.
 * If a mis-configuration is present, the {@link RegisterLoadAction} will emit an appropriate warning.
 *
 * @see Register
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AutoRegister {

}
