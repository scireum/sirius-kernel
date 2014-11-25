/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import javax.annotation.Nonnull;
import javax.script.ScriptContext;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Provides an execution context to scripts etc.
 * <p>
 * This is basically a wrapper for <code>Map&lt;String, Object&gt;</code>
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public class Context implements Map<String, Object> {

    protected Map<String, Object> data = new TreeMap<String, Object>();

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return data.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return data.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return data.get(key);
    }

    /**
     * Provides the value associated with the given key as {@link Value}
     *
     * @param key the key for which the value should ne returned
     * @return a value wrapping the internally associated value for the given key
     */
    @Nonnull
    public Value getValue(Object key) {
        return Value.of(get(key));
    }

    @Override
    public Object put(String key, Object value) {
        set(key, value);
        return value;
    }

    /**
     * Sets the given value (its string representation), mit limits this to <tt>limit</tt> characters.
     *
     * @param key   the key to which the value should be associated
     * @param value the value which string representation should be put into the context
     * @param limit the maximal number of characters to put into the map. Everything after that will be discarded
     */
    public void putLimited(String key, Object value, int limit) {
        set(key, Strings.limit(value, limit));
    }

    @Override
    public Object remove(Object key) {
        return data.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
        data.putAll(m);
    }

    @Override
    public void clear() {
        data.clear();
    }

    @Override
    public Set<String> keySet() {
        return data.keySet();
    }

    @Override
    public Collection<Object> values() {
        return data.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return data.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        return data.equals(o);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    /**
     * Creates a new context
     *
     * @return a newly created and empty context
     */
    public static Context create() {
        return new Context();
    }


    /**
     * Associates the given <tt>value</tt> to the given <tt>key</tt>, while returning <tt>this</tt>
     * to permit fluent method chains
     *
     * @param key   the key to which the value will be bound
     * @param value the value to be associated with the given key
     * @return <tt>this</tt> to permit fluent method calls
     */
    public Context set(String key, Object value) {
        data.put(key, value);
        return this;
    }

    /**
     * Puts all name-value-pairs stored in the given map.
     *
     * @param data the name value pairs to be put into the context
     * @return <tt>this</tt> to permit fluent method calls
     */
    public Context setAll(Map<String, Object> data) {
        putAll(data);
        return this;
    }

    /**
     * Writes all parameters into the given {@link javax.script.ScriptContext}
     *
     * @param ctx the context to be filled with the internally stored name value pairs
     */
    public void applyTo(ScriptContext ctx) {
        for (Entry<String, Object> e : entrySet()) {
            ctx.setAttribute(e.getKey(), e.getValue(), ScriptContext.ENGINE_SCOPE);
        }
    }

    @Override
    public String toString() {
        return "Context: [" + data + "]";
    }
}
