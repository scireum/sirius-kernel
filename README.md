# sirius-kernel
![sirius](docs/sirius.jpg)
[![Build Status](https://drone.scireum.com/api/badges/scireum/sirius-kernel/status.svg?ref=refs/heads/main)](https://drone.scireum.com/scireum/sirius-kernel)

Welcome to the **kernel module** of the SIRIUS OpenSource framework created by [scireum GmbH](https://www.scireum.de).
To learn more about what SIRIUS is please refer to the following links:

* [Why SIRIUS](docs/why.md)
* [Overview of SIRIUS](docs/overview.md)
* [How to obtain SIRIUS](docs/usage.md)
* [License](docs/license.md)
* [How to contribute](docs/contributions.md)

# SIRIUS Kernel Module

Being the foundation of all other modules, the kernel module has a minimal set of dependencies but also provides
the core frameworks and a fair amount of commonly used classes.

## Important files of this module:

* [Default configuration](src/main/resources/component-050-kernel.conf)
* [Maven setup](pom.xml).

## The Kernel

The main classes of SIRIUS are [Sirius](src/main/java/sirius/kernel/Sirius.java) and
[Setup](src/main/java/sirius/kernel/Setup.java). The former mainly controls the framework bootstrap and shutdown
process and the latter determines how exactly things are handled. The setup class can be subclassed to customize things
like the location of config files or the target directory of logs.

This is especially useful if SIRIUS is embedded in an application and manually started / stopped with its helper
methods. If SIRIUS itself launches the application, it is strongly recommended to use the standard implementation.
In this scenario one can use the *main* method of the **Setup** class to launch SIRIUS in the **IDE** or use
[IPL](https://github.com/scireum/docker-sirius-runtime/blob/master/src/main/java/IPL.java) via the supplied *docker*
runtime to build an image.

Note that [Sirius](src/main/java/sirius/kernel/Sirius.java) itself doesn't do anything more than launching all
frameworks in an appropriate order. Once the [Dependency Injection Microkernel](src/main/java/sirius/kernel/di) is up
and running, it uses this framework to execute all [startup](src/main/java/sirius/kernel/Startable.java) actions
supplied by the modules or the application.

Note that the [Classpath](src/main/java/sirius/kernel/Classpath.java) also represents a central class of SIRIUS but
will most commonly not be used directly. Its only job is to iterate over the whole classpath and enumerate resources
(or classes) matching a given filter query. In order to control which JARs and other classpath roots are scanned during
the startup process, each SIRIUS module and the application itself must place a file named **component.marker** in
its resources folder so that it ends up in the top-level directory of the resulting JAR file. The file can be left
empty.

More documentation for the core classes can be found here: [Core Classes](src/main/java/sirius/kernel)

## Customizations

Customizations are a central feature of SIRIUS as they permit to ship the same software to different customers as some
functionalities, settings or customer specific overwrites can be disabled or enabled by setting a configuration key.

Each customization can provide its own configuration, classes and resources. These must be placed in a sub-folder
or package of *customizations*. So if a customization would be named *foo** the base package would be
**customizations.foo** and the resources root **customizations/foo/**. Also, custom settings can be provided via
**customizations/foo/settings.conf**.

The framework will only pick up classes, resources and settings of enabled customizations. Also, the order of these
is defined, therefore one customization can further customize / overwrite others.

To set up which customizations are enabled in what order specify an array for *sirius.customizations* in the
**instance.conf**.

## Frameworks

* [Common Classes](src/main/java/sirius/kernel/commons)\
  Provides a bunch of commonly used classes. Get familiar with the helpers provided here as they are used
  throughout the framework.
* [Dependency Injection Microkernel](src/main/java/sirius/kernel/di)\
  Provides the dependency injection framework which also supports the **discovery based programming** paradigm.
  This is a _must-read_ for users of SIRIUS as this pattern is used in almost every framework.
* [Asynchronous Execution Framework](src/main/java/sirius/kernel/async)\
  Responsible for configuring and managing all thread pools and background facilities within SIRIUS.
* [System Configuration Framework](src/main/java/sirius/kernel/settings)\
  Uses the _config_ library supplied by **typesafe**. This sets up the configuration for all frameworks
  by evaluation the hierarchy of configuration files.
* [Cache Framework](src/main/java/sirius/kernel/cache)\
  Provides a thin layer above the LRU caches provided by Guava. Mainly this helps monitor
  the cache utilization and providing a uniform configuration using the **System Configuration Framework**.
* [NLS Framework](src/main/java/sirius/kernel/nls)\
  Provides various helpers to simplify internationalization and formatting of strings.
* [Timer Framework](src/main/java/sirius/kernel/timer)\
  Responsible for discovering and executing certain tasks in regular intervals.
* [XML Framework](src/main/java/sirius/kernel/xml)\
  Supplies helper classes to generate and process huge XML files without running into memory issues or giving up
  convenience.
* [System Health Framework](src/main/java/sirius/kernel/health)\
  Provides the foundations of the built-in console, metrics monitoring and the central logging and exception handling
  facility.
* [System Information Framework](src/main/java/sirius/kernel/info)\
  Provides some static information which has been assembled at compile time.

## Testing

Tests are based on **spock** and written in **Groovy**, a base specification providing a proper setup of the
system can be found in [BaseSpecification](src/test/java/sirius/kernel/BaseSpecification.groovy).

Our **golden rule** for tests is:
> _No matter if you start the whole test suite, a single specification or just
as single test (method) - the tests have to function independently of their surroundings. Therefore, a test
has to either succeed in all three scenarios or it must fail each time. Everything else indicates an invalid test
setup._

Each module and application should provide its own test suite as subclass of
[ScenarioSuite](src/test/java/sirius/kernel/ScenarioSuite.java).
See [TestSuite](src/test/java/TestSuite.java) as an example.

For testing, we heavily rely on **Docker** (especially when external systems like databases are required).
SIRIUS has a build-in helper to start and stop **docker-compose** setups.
See [DockerHelper](src/main/java/sirius/kernel/DockerHelper.java) for a list of supported configuration
settings or refer the [test setup](https://github.com/scireum/sirius-db/tree/main/src/test/resources)
of [sirius-db](https://github.com/scireum/sirius-db) and its
[TestSuite](https://github.com/scireum/sirius-db/blob/main/src/test/java/TestSuite.java) as an elaborate example.
