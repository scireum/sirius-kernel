/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

/**
 * Provides the main setup classes for the SIRIUS kernel.
 * <p>
 * In order to start and stop the SIRIUS framework, {@link sirius.kernel.Sirius#start(sirius.kernel.Setup)} and
 * {@link sirius.kernel.Sirius#stop()} can be invoked. {@link sirius.kernel.Setup} can be subclassed to
 * provide further tweaking of the framework behaviour.
 * <p>
 * The {@link sirius.kernel.Classpath} is responsible for discovering all classes and resources within the
 * classpath of the application. It will scan all classpath roots which contain a <tt>component.marker</tt>
 * file.
 *
 * <h3>Discovery Based Programming</h3>
 * The whole SIRIUS framework ist built around the concept of <b>Discovery Based Programming</b>. Therefore
 * many components don't actively call other services but rather implement an interface. This is
 * <b>discovered</b> at runtime and the appropriate service is activated.
 * <p>
 * An example is the {@link sirius.kernel.timer.TimerService} which calls components in regular intervals.
 * In order to use this service, an appropriate interface like {@link sirius.kernel.timer.EveryMinute}
 * has be implemented. Everything else is take care of be the framework.
 * <p>
 * In order to support discovery for an implementation the class has to wear a {@link sirius.kernel.di.std.Register}
 * annotation. To provide your own services utilizing discovery, fields can be annotated with
 * {@link sirius.kernel.di.std.Parts} which is then automatically filled with all components implementing the given
 * interface.
 * <p>
 * Note that the <b>Part</b>/<b>Register</b> mechanism can also be used for classic <b>Dependency Injection</b>
 * (Filling a single dependency at runtime just referring to it by an interface.)
 * <p>
 * Also note that the annotations <tt>Part</tt>, <tt>Parts</tt> and <tt>Register</tt> are itself extensions
 * to the IoC (inversion of control) micro kernel (see {@link sirius.kernel.di.std}).
 * Therefore own annotations can be easily added.
 *
 * <h3>System Configuration</h3>
 * SIRIUS provides a flexible way of building up a system configuration using the <b>Typesafe Config Library</b>. The
 * config can be accessed in three ways. The raw configuration is available via
 * {@link sirius.kernel.Sirius#getConfig()}. Also the annotation {@link sirius.kernel.di.std.ConfigValue} can be
 * placed on a field of a registered component to automatically fill it with the appropriate config value.
 * <p>
 * The third way of accessing the system config is the <b>Extensions</b> mechanism. This is done via the
 * {@link sirius.kernel.extensions.Extensions} class (or the {@link sirius.kernel.di.std.ExtensionList}
 * annotation placed on a field).
 * <p>
 * Extensions provide a way to have several config files contribute to one section which is then consumed by a service.
 * An example of this would be the {@link sirius.kernel.cache.CacheManager} which permits to define caches used
 * throughout the application. To declare the behaviour of a {@link sirius.kernel.cache.Cache} an extension
 * has to be placed in any config file which contributes to <tt>cache.[name]</tt>:
 * <pre>
 * <code>
 * cache {
 *    my-cache {
 *         maxSize = 100
 *         ttl = 1 hour
 *    }
 * }
 * </code>
 * </pre>
 * <p>
 * If a value is omitted the default value (in this case defined in <tt>component-kernel.conf</tt>) will be applied.
 * <p>
 * The order in which the configs are loaded is the following (can be customized by overriding
 * {@link sirius.kernel.Setup}) - config files can always redefine values of their predecessors:
 * <ol>
 *     <li>Each component-[module].conf found for a classpath root (detected via a <tt>component.marker</tt>)</li>
 *     <li>application.conf (unless running as test)</li>
 *     <li>develop.conf - if running in <b>debug</b> mode and not running as unit test</li>
 *     <li>Each settings.conf for all active customizations (see below)</li>
 *     <li>instance.conf - This file is not loaded from the classpath but form the local file system (where the app is started)</li>
 * </ol>
 * <p>
 * Using this layout, all default values can be declared in the appropriate <tt>component-[name].conf</tt> rather
 * than be buried somewhere in the Java code. This makes maintaining and discovering them quite easy.
 *
 * <h3>Frameworks</h3>
 * SIRIUS modules are split up into frameworks which can be dis- or enabled. The kernel module for example provides
 * <tt>timer</tt> as a framework. So if an application does not require a timer service, this framework can be
 * disabled by setting <code>sirius.frameworks.kernel.timer = false</code>. By default this the timer is
 * active (see component-kernel.conf). To provide custom frameworks which can be dis- or enabled a default value
 * has to be defined in the <tt>component-[name].conf</tt>). Then it can be used as
 * {@link sirius.kernel.di.std.Register#framework()} in the appropriate components. If the framework is not enabled,
 * these component will be ignored by SIRIUS.
 *
 * <h3>Customizations</h3>
 * Often it is necessary to add or change the behaviour of an application for specific customers. In order to still
 * keep the whole code in one code base (which permits refactorings etc.), SIRIUS supports <b>customizations</b>.
 * <p>
 * A customization is defined by putting all classes into a package named <tt>customizations.[name]</tt>. All
 * resources for this customization have to be placed in the folder <tt>customizations/[name]</tt>. Now analog to
 * <b>frameworks</b> the config property <code>sirius.customizations</code> - which is a list, so that several
 * customizations can be active at once.
 * <p>
 * Now if a customization is active, all resources present <b>override</b> the default resources and all classes
 * wearing a {@link sirius.kernel.di.std.Register} annotation are loaded. Note that a customization can even
 * re-register and therefore replace a default component registered for the same type. Finally, the
 * <tt>settings.conf</tt> within the customization is loaded (overriding application.conf, but not instance.conf).
 */
package sirius.kernel;