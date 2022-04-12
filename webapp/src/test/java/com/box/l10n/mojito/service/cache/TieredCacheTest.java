package com.box.l10n.mojito.service.cache;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.cache.concurrent.ConcurrentMapCache;

/**
 * @author garion
 */
public class TieredCacheTest {
    public final String TEST_CACHE_NAME = "tieredCache";
    public final String KEY = "key";
    public final String KEY2 = "key2";
    public final String VALUE = "value";
    public final String VALUE2 = "value2";

    @Test
    public void nullTier2CacheWorks() {
        ConcurrentMapCache t1Cache = new ConcurrentMapCache("t1");

        TieredCache tieredCache = new TieredCache(TEST_CACHE_NAME, t1Cache, null);

        tieredCache.put(KEY, VALUE);
        Assertions.assertEquals(VALUE, tieredCache.get(KEY).get());
        Assertions.assertEquals(VALUE, t1Cache.get(KEY).get());

        tieredCache.put(KEY, VALUE2);
        Assertions.assertEquals(VALUE2, tieredCache.get(KEY).get());
        Assertions.assertEquals(VALUE2, t1Cache.get(KEY).get());
        Assertions.assertEquals(1, t1Cache.getNativeCache().size());

        tieredCache.put(KEY2, VALUE2);
        Assertions.assertEquals(VALUE2, tieredCache.get(KEY2).get());
        Assertions.assertEquals(VALUE2, t1Cache.get(KEY2).get());
        Assertions.assertEquals(2, t1Cache.getNativeCache().size());

        tieredCache.evict(KEY2);
        Assertions.assertNull(t1Cache.get(KEY2));
        Assertions.assertEquals(1, t1Cache.getNativeCache().size());

        tieredCache.clear();
        Assertions.assertNull(t1Cache.get(KEY));
        Assertions.assertNull(t1Cache.get(KEY2));
        Assertions.assertEquals(0, t1Cache.getNativeCache().size());
    }

    @Test
    public void tieredCacheWorks() {
        ConcurrentMapCache t1Cache = new ConcurrentMapCache("t1");
        ConcurrentMapCache t2Cache = new ConcurrentMapCache("t2");

        TieredCache tieredCache = new TieredCache(TEST_CACHE_NAME, t1Cache, t2Cache);

        tieredCache.put(KEY, VALUE);
        Assertions.assertEquals(VALUE, tieredCache.get(KEY).get());
        Assertions.assertEquals(VALUE, t1Cache.get(KEY).get());
        Assertions.assertEquals(VALUE, t2Cache.get(KEY).get());

        tieredCache.put(KEY, VALUE2);
        Assertions.assertEquals(VALUE2, tieredCache.get(KEY).get());
        Assertions.assertEquals(VALUE2, t1Cache.get(KEY).get());
        Assertions.assertEquals(VALUE2, t2Cache.get(KEY).get());
        Assertions.assertEquals(1, t1Cache.getNativeCache().size());
        Assertions.assertEquals(1, t2Cache.getNativeCache().size());

        tieredCache.put(KEY2, VALUE2);
        Assertions.assertEquals(VALUE2, tieredCache.get(KEY2).get());
        Assertions.assertEquals(VALUE2, t1Cache.get(KEY2).get());
        Assertions.assertEquals(VALUE2, t2Cache.get(KEY2).get());
        Assertions.assertEquals(2, t1Cache.getNativeCache().size());
        Assertions.assertEquals(2, t2Cache.getNativeCache().size());

        tieredCache.evict(KEY2);
        Assertions.assertNull(t1Cache.get(KEY2));
        Assertions.assertNull(t2Cache.get(KEY2));
        Assertions.assertEquals(1, t1Cache.getNativeCache().size());
        Assertions.assertEquals(1, t2Cache.getNativeCache().size());

        tieredCache.clear();
        Assertions.assertNull(t1Cache.get(KEY));
        Assertions.assertNull(t1Cache.get(KEY2));
        Assertions.assertNull(t2Cache.get(KEY));
        Assertions.assertNull(t2Cache.get(KEY2));
        Assertions.assertEquals(0, t1Cache.getNativeCache().size());
    }

    @Test
    public void tieredCacheTier1UpdateWorks() {
        ConcurrentMapCache t1Cache = new ConcurrentMapCache("t1");
        ConcurrentMapCache t2Cache = new ConcurrentMapCache("t2");

        TieredCache tieredCache = new TieredCache(TEST_CACHE_NAME, t1Cache, t2Cache);

        t1Cache.put(KEY, VALUE);
        Assertions.assertEquals(VALUE, tieredCache.get(KEY).get());
        Assertions.assertEquals(VALUE, t1Cache.get(KEY).get());
        Assertions.assertNull(t2Cache.get(KEY));
    }

    @Test
    public void tier1CacheMissGetsPopulatedOnGet() {
        ConcurrentMapCache t1Cache = new ConcurrentMapCache("t1");
        ConcurrentMapCache t2Cache = new ConcurrentMapCache("t2");

        TieredCache tieredCache = new TieredCache(TEST_CACHE_NAME, t1Cache, t2Cache);

        t2Cache.put(KEY, VALUE);
        Assertions.assertEquals(VALUE, tieredCache.get(KEY).get());
        Assertions.assertEquals(VALUE, t1Cache.get(KEY).get());
    }

    @Test(expected = NullPointerException.class)
    public void missingCacheNameThrows() {
        ConcurrentMapCache t1Cache = new ConcurrentMapCache("t1");
        ConcurrentMapCache t2Cache = new ConcurrentMapCache("t2");
        new TieredCache(null, t1Cache, t2Cache);
    }

    @Test(expected = NullPointerException.class)
    public void missingTier1CacheThrows() {
        ConcurrentMapCache t2Cache = new ConcurrentMapCache("t2");
        new TieredCache(TEST_CACHE_NAME, null, t2Cache);
    }
}