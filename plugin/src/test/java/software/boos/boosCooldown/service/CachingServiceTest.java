package software.boos.boosCooldown.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CachingServiceTest {

    @Test
    void getComputesValueOnMiss() {
        CachingService<String, String> cache = new CachingService<>(60_000);
        assertEquals("computed", cache.get("k", key -> "computed"));
    }

    @Test
    void getUsesCachedValueOnHit() {
        CachingService<String, String> cache = new CachingService<>(60_000);
        AtomicInteger calls = new AtomicInteger();
        cache.get("k", key -> { calls.incrementAndGet(); return "v"; });
        cache.get("k", key -> { calls.incrementAndGet(); return "v"; });
        assertEquals(1, calls.get());
    }

    @Test
    void getReloadsAfterExpiration() throws InterruptedException {
        CachingService<String, Integer> cache = new CachingService<>(20);
        AtomicInteger value = new AtomicInteger(1);
        assertEquals(1, cache.get("k", key -> value.get()));
        Thread.sleep(40);
        value.set(2);
        assertEquals(2, cache.get("k", key -> value.get()));
    }

    @Test
    void putRemovesWhenValueIsNull() {
        CachingService<String, String> cache = new CachingService<>(60_000);
        cache.put("k", "v");
        cache.put("k", null);
        assertNull(cache.get("k", key -> null));
    }

    @Test
    void removeEvictsKey() {
        CachingService<String, String> cache = new CachingService<>(60_000);
        cache.put("k", "v");
        cache.remove("k");
        AtomicInteger calls = new AtomicInteger();
        cache.get("k", key -> { calls.incrementAndGet(); return "v"; });
        assertEquals(1, calls.get());
    }

    @Test
    void clearEvictsEverything() {
        CachingService<String, String> cache = new CachingService<>(60_000);
        cache.put("a", "1");
        cache.put("b", "2");
        assertEquals(2, cache.size());
        cache.clear();
        assertEquals(0, cache.size());
    }

    @Test
    void removeIfEvictsMatching() {
        CachingService<String, String> cache = new CachingService<>(60_000);
        cache.put("a", "1");
        cache.put("b", "2");
        cache.put("c", "3");
        assertTrue(cache.removeIf(key -> key.compareTo("b") >= 0));
        assertEquals(1, cache.size());
    }

    @Test
    void cleanUpRemovesExpiredOnly() throws InterruptedException {
        CachingService<String, String> cache = new CachingService<>(20);
        cache.put("short", "x");
        cache.put("long", "y", 60_000);
        Thread.sleep(40);
        cache.cleanUp();
        assertEquals(1, cache.size());
        assertNotNull(cache.get("long", k -> "fallback"));
        assertFalse("fallback".equals(cache.get("long", k -> "fallback")));
    }
}
