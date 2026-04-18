package software.boos.boosCooldown.service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * A thread-safe caching service for storing and retrieving values.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class CachingService<K, V> {
    private final Map<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
    private final long defaultTtl;

    /**
     * Creates a new CachingService with the specified default TTL.
     *
     * @param defaultTtl the default time to live in milliseconds
     */
    public CachingService(long defaultTtl) {
        this.defaultTtl = defaultTtl;
    }

    /**
     * Gets a value from the cache, computing it if necessary.
     *
     * @param key the key whose associated value is to be returned
     * @param valueLoader the function to compute the value if it's not in the cache
     * @return the current (existing or computed) value associated with the specified key
     */
    public V get(K key, Function<K, V> valueLoader) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(valueLoader, "Value loader cannot be null");

        CacheEntry<V> entry = cache.compute(key, (k, existing) -> {
            if (existing != null && !existing.isExpired()) {
                return existing;
            }
            V value = valueLoader.apply(k);
            return value != null ? new CacheEntry<>(value, defaultTtl) : null;
        });

        return entry != null ? entry.getValue() : null;
    }

    /**
     * Puts a value in the cache with the default TTL.
     *
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     */
    public void put(K key, V value) {
        put(key, value, defaultTtl);
    }

    /**
     * Puts a value in the cache with a custom TTL.
     *
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @param ttl the time to live in milliseconds
     */
    public void put(K key, V value, long ttl) {
        Objects.requireNonNull(key, "Key cannot be null");
        if (value == null) {
            cache.remove(key);
        } else {
            cache.put(key, new CacheEntry<>(value, ttl));
        }
    }

    /**
     * Removes the value associated with the specified key.
     *
     * @param key the key whose mapping is to be removed from the cache
     */
    public void remove(K key) {
        cache.remove(key);
    }

    /**
     * Removes all entries from the cache.
     */
    public void clear() {
        cache.clear();
    }
    
    /**
     * Removes all entries that match the given predicate.
     *
     * @param filter a predicate which returns true for entries to be removed
     * @return true if any elements were removed
     */
    public boolean removeIf(java.util.function.Predicate<K> filter) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        return cache.keySet().removeIf(key -> {
            boolean shouldRemove = filter.test(key);
            if (shouldRemove) {
                // Log at FINE level if you have a logger available
                System.out.println("[CachingService] Removing cache entry for key: " + key);
            }
            return shouldRemove;
        });
    }

    /**
     * Removes all expired entries from the cache.
     */
    public void cleanUp() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * Gets the number of entries in the cache.
     *
     * @return the number of entries in the cache
     */
    public int size() {
        return cache.size();
    }

    private static class CacheEntry<V> {
        private final V value;
        private final long expiryTime;

        CacheEntry(V value, long ttl) {
            this.value = value;
            this.expiryTime = System.currentTimeMillis() + ttl;
        }

        V getValue() {
            return value;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
}
