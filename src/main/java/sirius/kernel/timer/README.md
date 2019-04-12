# System Timers

Provides a set of timers which are invoked in regular intervals.

Subclasses of [EveryDay](EveryDay.java), [EveryHour](EveryHour.java), [EveryTenMinutes](EveryTenMinutes.java),
[EveryMinute](EveryMinute.java) and [EveryTenSeconds](EveryTenSeconds.java) are invoked in the appropriate
interval.
 
Note that all these directly run in the timer thread and must not block it. Therefore it is
strongly advised to use [Tasks](../async/Tasks.java) to perform the execution in a separate
threads.

Also note, that work which has to be executed in the background in regular intervals can be done
in [background loops](../async/BackgroundLoop.java). These are load aware (won't start a 2nd job if the first is 
still running) and can be even managed and orchestrated across clusters.
