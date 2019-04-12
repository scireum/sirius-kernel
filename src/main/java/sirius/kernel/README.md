# Core Classes

Next to the sub packages provided, this contains three core frameworks / facilities.

* [Auto Setup Facility](AutoSetup.java)\
Executes all [AutoSetupRules](AutoSetupRule.java) on startup if **sirius.autoSetup** is set to
true in the system configuration. These rules should check the system consistency and create missing data objects
if necessary (e.g. system tenants).

* [Start](Startable.java)/[Stop](Stoppable.java) Facility\
All startable tasks will be started on system startup. All stoppable tasks will be invoked
on system shutdown. Once completed, all killable tasks will be invoked to *really* stop them
if they didn't respont to their stop call.

* [Docker Facility](DockerHelper.java)\
Starts/Stops docker containers. This is mainly used for test- and development systems and
cann start required docker containers (databases, storage systems ..).\
Use **docker.file** to specify a *docker-compose* file which will be started. Specify
a **docker.project** so that all containers keep their name (for dev environments).
Otherwise each start will launch a new set of containers which is suitable for test runs.\
Note that this provides a [PortMapper](settings/PortMapper.java) so that ports in the
system configuration can automatically mapped to the ports dynamically assigned
by docker-compose.
