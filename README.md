![sirius](docs/sirius.jpg)

Welcome to the **kernel module** of the SIRIUS OpenSource framework created by [scireum GmbH](https://www.scireum.de). 
To learn more about what sirius is please refer to the following links:

* [Why SIRIUS](docs/why.md)
* [Overview of SIRIUS](docs/overview.md)
* [How to obtain SIRIUS](docs/usage.md)
* [License](docs/license.md)
* [How to contribute](docs/contributions.md)

# SIRIUS Kernel Module

Being the foundation of all other modules, the kernel module has a minimal set of dependencies but also provides
the core frameworks and a fair amount of commonly used classes.

Important files of this module: [Default configuration](src/main/resources/component-kernel.conf), [Maven setup](pom.xml). 

## The Kernel

## Customizations

## Frameworks

* [Common Classes](src/main/java/sirius/kernel/commons)\
Provides a bunch of commonly used classes. Get familiar with the helpers provided here as they are used 
throughout the framework.  
* [Dependency Injection Microkernel](src/main/java/sirius/kernel/di)\
Provides the depence injection framework which also supports the **discovery based programming** paradigm.
This is a _must read_ for users of SIRIUS as this pattern is used in almost every framework.
* [Asynchronous Execution Framework](src/main/java/sirius/kernel/async)\
Responsible for configuring and managing all thread pools and background facilities within SIRIUS.
* [System Configuration Framework](src/main/java/sirius/kernel/settings)\
Uses the _config_ library supplied by **typesafe**. This sets up the configuration for all frameworks
by evaluation the hierarchy of configuration files.
* [Cache Framework](src/main/java/sirius/kernel/cache)\
Provides a thin layer above the LRU caches provided by Guava. Mainly this helps monitoring
the cache utilization and providing a uniform configuration using the **System Configuration Framework**.
* [NLS Framework](src/main/java/sirius/kernel/nls)\
Provides various helpers to simplify internationalization and formatting of strings.
* [Timer Framework](src/main/java/sirius/kernel/timer)\
Responsible for discovering and executing certain tasks in regular intervals.
* [XML Framework](src/main/java/sirius/kernel/timer)\
Supplies helper classes to generate and process huge XML files without running into memory issues or giving up convenience.
* [System Health Framework](src/main/java/sirius/kernel/health)\
Provides the foundations of the built-in console, metrics monitoring and the central logging and exception handling facility.
* [System Information Framework](src/main/java/sirius/kernel/info)\
Provides some static information which has been assembled at compile time.

## Testing

Tests are based on **spock** and written in **Groovy**, a base specification which proper setup of the
framework can be found in [BaseSpecification](src/test/java/sirius/kernel/BaseSpecification.groovy).

Each module and application should provide its own test suite as subclass of 
[ScenarioSuite](src/test/java/sirius/kernel/ScenarioSuite.java).
See [TestSuite](src/test/java/TestSuite.java) as an example.

For testing we heavily rely on **Docker** (especially when external systems like databases are required).
SIRIUS has a build-in helper to start and stop **docker-compose** setups. 
See [DockerHelper](src/main/java/sirius/kernel/DockerHelper.java) for a list of supported configuration
settings or refer the [test setup](https://github.com/scireum/sirius-db/tree/master/src/test/resources) 
of [sirius-db](https://github.com/scireum/sirius-db) and its 
[TestSuite](https://github.com/scireum/sirius-db/blob/master/src/test/java/TestSuite.java) as an elaborate example.
