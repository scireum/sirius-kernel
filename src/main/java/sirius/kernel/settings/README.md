# System configuration

Provides a wrapper around the [typesafe config library](https://github.com/typesafehub/config).

The system configuration can be accessed via `Sirius.getSettings` or via [ConfigValue](../di/std/ConfigValue.java)
annotations.

[ExtendedSettings](ExtendedSettings.java) provides various helper functions to extract single
config values or to read a single or all [extensions](Extension.java) for an extension point.
