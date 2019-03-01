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

## The Kernel

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
* [Cache Framework](src/main/java/sirius/kernel/async)\
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

## Testing


