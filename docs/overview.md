![sirius](sirius.jpg)

# Overview of SIRIUS

SIRIUS was originally created to be the core of highly scaleable web based SAAS
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

### sirius-kernel

### sirius-web

### sirius-db

### sirius-biz

## Utilities

### sirius-parent

### docker-sirius-runtime

### docker-sirius-build

## Discovery Based Programming

## Supporting Projects

### parsii

### server-sass

## OpenSource applications using SIRIUS

### s3ninja

### Woody

### Datadonkey
