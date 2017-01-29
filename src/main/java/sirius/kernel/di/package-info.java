/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

/**
 * Contains the micro kernel for the discovery based programming framework.
 * <h3>The Micro Kernel</h3>
 * This annotation based framework is the heart of SIRIUS. Most of the time, the annotations
 * {@link sirius.kernel.di.std.Register} will be used to put parts (components) into the injector and
 * {@link sirius.kernel.di.std.Part} or {@link sirius.kernel.di.std.Parts} will be used to retrieve (discover) those.
 * <p>
 * Note that all these annotations are not part of the micro kernel but already extensions to it (this is why it is
 * a micro kernel). Therefore own handlers for custom annotations or patterns can be implemented and treated as first
 * class citizens.
 * <h3>Adding Functionality</h3>
 * To provide such functionality, a {@link sirius.kernel.di.ClassLoadAction} or a
 * {@link sirius.kernel.di.FieldAnnotationProcessor} has to be implemented. Those implementation will be picked up
 * by Sirius during the start phase and therefore be automatically applied. All that is required is a file called
 * <tt>component.marker</tt> in the classpath root of the respective code base to make these and other classes
 * visible to Sirius.
 * <h3>Accessing Components</h3>
 * To access components without using annotations, the {@link sirius.kernel.di.Injector} class can be used. If
 * provides access to the {@link sirius.kernel.di.GlobalContext} which contains all registered parts (components).
 * Note that the <tt>GlobalContext</tt> can also be injected using a {@link sirius.kernel.di.std.Part} annotation.
 */
package sirius.kernel.di;