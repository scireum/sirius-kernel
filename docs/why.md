![sirius](sirius.jpg)

# Why SIRIUS

## Discovery Based Programming
SIRIUS not only supports *dependency injection* but it takes it to the next level. 
It embraces a programming style called **Discovery Based Programming** which permits 
to provide services that discover their users at runtime. Please refer to the
 [Dependency Injection Microkernel](../src/main/java/sirius/kernel/di) for further informaton.

## KISS
Keep it simple and stupid: No classloader ticks. No magic implicit variables coming from god knows where. 
No bytecode rewriting. No layers of indirection if not needed. 

## Maintainability
By providing loose coupling via IoC-Patterns (inversion of control) like discovery and dependency injection, 
SIRIUS permits to write modular, maintainable and testable software with clear responsibilities. 
    
## Use your IDE
SIRIUS can be started in any IDE/Debugger just like any normal Java appplication 
([Setup](../src/main/java/sirius/kernel/Setup.java) is actually a valid main class). As no classloader or bytecode 
magic is used, class reloading in the VM works a treat. To further save precious developer time, the framework starts 
ultra fast. Having a server up and running in less than two seconds drives productivity above level 9000!

## Focus on Software Products
There is quite a difference between software which is built and engineered for a single customer and software 
products which are used by many customers. The latter need more flexibility when it comes to configuration. Also a 
mechanism to manage customer extensions is required. Both is provided by the [SIRIUS Kernel](../). 
Also, the discovery based style of Sirius permits to reuse common services across multiple independent products in 
an easy way.


## Made for Production
Running critical servers in production environments forces you to instantly know, what's going on in the system. 
The exception handling system provided by SIRIUS provides excellent insight, while neither getting jammed by 
reoccurring errors nor missing any important messages. Leveraging the dependency injection framework, every application
creator can decide where and how to collect those errors. Using the built-in profiler, central activities can be 
inspected in running production systems with almost no overhead.
