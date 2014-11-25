/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

/**
 * Provides a framework for building and using caches.
 * <p>
 *     All caches are created using the {@link sirius.kernel.cache.CacheManager}
 * </p>
 *<p>
 *     Using the {@link sirius.kernel.cache.Cache} instead of simple maps provides various benefits. One is, that
 *     a cache as a limited size and will start to drop entries once it is full instead of using an uncontrolled
 *     large part of the heap. Additionally all caches are evicted regularly so that unused entries are removed to
 *     again free central resources.
 *</p>
 * <p>
 *     Also, all classes provide details of the current state of a cache, as well as the state of each entry which
 *     permits to monitor and understand the system state.
 * </p>
 */
package sirius.kernel.cache;