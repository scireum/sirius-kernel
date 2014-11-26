/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.cache;

import sirius.kernel.health.Counter;

import javax.annotation.Nullable;
import java.util.Date;

/**
 * Represents an entry of a <tt>Cache</tt>
 * <p>
 * Such entries are created and managed by the cache implementation. However, via {@link sirius.kernel.cache.Cache#getContents()}
 * can be used to access the entries of a cache. These entries provide detailed information for each item in the cache.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public class CacheEntry<K, V> {
    /*
     * Provides the number of hits.
     */
    protected Counter hits = new Counter();
    /*
     * Timestamp when the entry was added to the cached.
     */
    protected long created = 0;
    /*
     * Timestamp when the entry was last used.
     */
    protected long used = 0;
    /*
     * The key for this value
     */
    protected final K key;
    /*
     * The cached value.
     */
    protected V value;
    /*
     * Returns the max age of an entry.
     */
    protected long maxAge;
    /*
     * Timestamp of the next verification
     */
    protected long nextVerification;

    /**
     * Returns the number of "hits" of this entries
     *
     * @return reports how many times this entry was retrieved from the cache
     */
    public Counter getHits() {
        return hits;
    }

    /**
     * Returns the timestamp when this entry was created.
     *
     * @return the point in time, when this entry was created or put into the cache
     */
    public Date getCreated() {
        return new Date(created);
    }

    void setCreated(long created) {
        this.created = created;
    }

    /**
     * Returns the timestamp of the last access or use of this entry
     *
     * @return the date when the entry was last used
     */
    public Date getUsed() {
        return new Date(used);
    }

    /**
     * Returns the expiry date of this entry
     *
     * @return the timestamp when this entry will expire and be evicted
     */
    public Date getTtl() {
        return new Date(maxAge);
    }

    /*
     * Updates the number of uses
     */
    void setUsed(long used) {
        this.used = used;
    }

    /**
     * Provides access to the internally stored value
     *
     * @return the value which is cached
     */
    @Nullable
    public V getValue() {
        return value;
    }

    /*
     * Sets the internally stored value
     */
    void setValue(V value) {
        this.value = value;
    }

    /*
     * Creates a new entry based on the given parameters
     */
    CacheEntry(K key, V value, long maxAge, long nextVerification) {
        super();
        this.key = key;
        this.maxAge = maxAge;
        this.nextVerification = nextVerification;
        this.used = System.currentTimeMillis();
        this.created = used;
        this.value = value;
    }

    /*
     * Returns the expiry date for this entry
     */
    long getMaxAge() {
        return maxAge;
    }

    /*
     * Sets the expiry date for this entry
     */
    void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }

    /*
     * Returns the timestamp of the next verification
     */
    long getNextVerification() {
        return nextVerification;
    }

    /*
     * Sets the timestamp of the next verification
     */
    void setNextVerification(long nextVerification) {
        this.nextVerification = nextVerification;
    }

    /**
     * Returns the key associated with this entry
     *
     * @return the key for which the contained value was cached
     */
    public K getKey() {
        return key;
    }

    @Override
    public String toString() {
        return key + ": " + value;
    }
}
