# Caching

[Caches](Cache.java) can be created using the [CacheManager](CacheManager.java). Each cache
should be as constant (**static final**). The configuration of a cache is loaded from the
system configuration using **cache** section (see [component-kernel.conf](../../../../resources/component-kernel.conf)).

A coherent cache will be synchronized across a cluster with the help of [CacheCoherence](CacheCoherence.java) -
[sirius-biz](https://github.com/scireum/sirius-biz) provides an implementation for this using **Redis**.

For local caches of a single value an [InlineCache](InlineCache.java) can be created using `CacheManager.createInlineCache(..)`.

## Maintenance
 
The size, utilisation and hit rate of all cached can be shown using the [caches](../health/console/CacheCommand.java) 
command in a [system console](../health/console).
