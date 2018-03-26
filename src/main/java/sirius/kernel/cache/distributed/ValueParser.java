/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.cache.distributed;

/**
 * Parses values cached in distributed caches from and to json.
 *
 * @param <V> The type of the cached value.
 */
public interface ValueParser<V> {

    /**
     * Parses the JSON back to an object.
     *
     * @param json The JSON saved in the cache.
     * @return The parsed object from the json.
     */
    V toObject(String json);

    /**
     * Parses the object to JSON.
     *
     * @param object The object which will be saved in the cache.
     * @return The parsed JSON from the object.
     */
    String toJSON(V object);
}
