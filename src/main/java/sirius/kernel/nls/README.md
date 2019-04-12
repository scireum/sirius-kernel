# Native Language Support

This framework provides is responsible for loading all available **.properties**
files in the resources folder of modules. They are discovered using the [Classpath](../Classpath.java).

## Translation

The workhorse of the framework is [NLS](NLS.java) which provides access to all translations via
`NLS.get("My.property")`. For structured messages `NLS.fmtr("My.property").set("param", value).format()` can be used.
In contrast to classic patterns used by the JDK (like `I am {0}`) the format used here (`I am ${username}`) is way more
appealing for professional translation services. Also, passing parameters by name makes the code more
understandable.

## Formatting

Additionally the **NLS** helper provides various format helpers for decimal, date and time formats.
These can be accessed individually or used via `NLS.toUserString(x)` or its counterpart `NLS.parseUserString(x)`.

## Developer support

If the **debug** environment variable is set, all **.properties** files are monitored and
reloaded on change.

Also, the framework keeps track of unknown translations and reports these. A missing translation will
also fail any unit test automatically.

Next to that, the framework also warns if a property is defined more than once.
