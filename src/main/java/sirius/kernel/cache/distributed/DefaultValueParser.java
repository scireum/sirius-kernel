/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.cache.distributed;

import com.alibaba.fastjson.JSON;

/**
 * Simple value parser utilizing the {@link JSON} parsing utilities.
 *
 * @param <V> The type of the cached value.
 */
public class DefaultValueParser<V> implements ValueParser<V> {

    private Class<V> clazz;

    public DefaultValueParser(Class<V> clazz) {
        this.clazz = clazz;
    }

    @Override
    public V toObject(String json) {
        return JSON.parseObject(json, clazz);
    }

    @Override
    public String toJSON(V object) {
        return JSON.toJSONString(object);
    }
}
