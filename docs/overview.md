![sirius](sirius.jpg)

# Overview of SIRIUS

SIRIUS was originally created to be the core of highly scaleable web based SaaS
applications. While this is still one of its main use cases and the driver behind
many of the frameworks being developed, it also found its use in several other
software projects.

Therefore SIRIUS is split into a set of distinct modules. The main objective is to
minimize the dependencies required by a module and thus by an application which ues
one or more SIRIUS modules.

To further increase the control of what is running within SIRIUS, some modules are
split into several frameworks which can be enabled or disabled independently.
Especially [sirius-biz](https://github.com/scireum/sirius-biz) makes heavy use of 
this as it provides quite a range of functionalities for several different databases.

## Modules

As SIRIUS can be used from simple CLI projects up to full blown SaaS application clusters,
it is split into several modules which each bring their of set of capabilities and dependencies.

### sirius-kernel

The kernel module bundles common helper classes along with the central dependency injection
microkernel. It tries to maintain a minimal set of runtime dependencies to make it universally usable.

### sirius-web

The web module provides a HTTP server based on **netty**. It also contains our modern and statically checked
template engine **Tagliatelle** which is heavily used in our web based applications. Furthermore
a layer above *Javamail* is provided to simplify sending mails.

More about the module can be found on GitHub: [scireum/sirius-web](https://github.com/scireum/sirius-web)

### sirius-db

The database module takes care of accessing different databases. Currently JDBC, MongoDB, Elasticsearch and Redis are supported.
For all three a low-level interface is provided which supports ease of use with next to none performance implications.
Additionally **Mixing**, out next-gen ORM/Entity Mapper provides simple and efficient access to the databases using
Java classes.

More about the module can be found on GitHub: [scireum/sirius-db](https://github.com/scireum/sirius-db)

### sirius-biz

The "biz" module is the foundation for building web based applications. Thus it combines
the power of **sirius-web** with **sirius-db**. Furthermore it provides heaps of functionalities and
frameworks like a mulit tenant user management or a log framework (for system-, change- and security logs).

Additionally a cluster management layer (using Redis) is provided which supports cache coherence, distributed
task queues and the like.

More about the module can be found on GitHub: [scireum/sirius-biz](https://github.com/scireum/sirius-biz)

## Utilities

### docker-sirius-runtime

SIRIUS can be started as standalone application (using the [Setup class](../src/main/java/sirius/kernel/Setup.java)).
To provide a defined environment, this repository yields a Docker image
which contains a bootloader and the appropriate JDK to run a SIRIUS based application.

More about the image can be found on GitHub: [scireum/docker-sirius-runtime](https://github.com/scireum/docker-sirius-runtime).

### docker-sirius-build

Contains a Docker image which is be used to build SIRIUS modules or SIRIUS based applications.
This can be used for Docker based CI systems (like *Drone*).

More about the image can be found on GitHub: [scireum/docker-sirius-build](https://github.com/scireum/docker-sirius-build).

## Supporting Projects

Our OpenSource effort doesn't stop here. We provide additional libraries which
are also itself used by SIRIUS.

### parsii

**parsii** is still one of the fastest expression parsers and evaluators (written in Java - without compiling to bytecode).
We used it in **Tagliatelle** to compile Java like expressions in templates.

More about parsii can be found on GitHub: [scireum/parsii](https://github.com/scireum/parsii).

### server-sass

Provides a server sided compiler for translating **SASS** expressions to **CSS**. This is used
by **sirius-web**, which provides automatic translation for SASS templates.

More about server-sass can be found on GitHub: [scireum/server-sass](https://github.com/scireum/server-sass).

## OpenSource applications using SIRIUS

SIRIUS isn't just used in our closed source products. Some of them are OpenSource
and therefore good examples of how to use SIRIUS for production quality applications.

### s3ninja

Provides a mock for the Amazon S3 API. We use s3ninja in all our development and test setups
so that our unit tests to crush our AWS bill. Of course s3 ninja is also available as *Docker* image.

More about server-sass can be found on GitHub: [scireum/s3ninja](https://github.com/scireum/s3ninja).

### Woody

We built our own CRM system which does everything from managing contact data to accounting
and lead management. This effort is made available for everyone to be used or - even better - to be cutomized and fine tuned.

More about server-sass can be found on GitHub: [scireum/woody](https://github.com/scireum/woody).

### Datadonkey

SIRIUS itself provide quite some capabilities to read / write and convert huge XML, CSV or XLS files.
Sometime this functionality comes in handy when working which such files.
Therefore **Datadonkey* makes them available via simple *JavaScripts*.

More about Datadonkey can be found on GitHub: [scireum/datadonkey](https://github.com/scireum/datadonkey).

