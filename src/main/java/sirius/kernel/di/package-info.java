/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

/**
 * Micro kernel based dependency injection framework.
 * <p>
 *     This annotation based di framework is the heart of SIRIUS. Most of the time, the annotations
 *     {@link sirius.kernel.di.std.Register} will be used to put parts (dependencies) into the injector and
 *     {@link sirius.kernel.di.std.Part} or {@link sirius.kernel.di.std.Parts} will be used to retrieve those.
 * </p>
 * <p>
 *     Additionally the configuration can be accessed via {@link sirius.kernel.di.std.ConfigValue} and
 *     {@link sirius.kernel.di.std.ExtensionList}. The kernel itself ({@link sirius.kernel.di.GlobalContext}) can
 *     be accessed either via {@link sirius.kernel.di.std.Context} or without any annotations using:
 *     {@link sirius.kernel.di.Injector}.
 * </p>
 * <p>
 *     Being a micro kernel, all those annotations are already extensions. Therefore it's very easy to process other
 *     annotations (i.e. like Java's <tt>javax.inject.Inject</tt>).
 * </p>
 * <p>
 *     The framework operates on field level. Therefore an object can be "wired" (having its dependencies filled), which
 *     fills fields with the appropriate values. Static fields are filled on system startup. All parts will be
 *     automatically wired when they are inserted into the context. Other objects can be manually wired by calling
 *     {@link sirius.kernel.di.GlobalContext#wire(Object)}.
 *     However, this framework does neither support nor use dependency injection in constructors.
 * </p>
 */
package sirius.kernel.di;