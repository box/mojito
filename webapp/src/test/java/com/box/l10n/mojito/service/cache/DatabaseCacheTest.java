package com.box.l10n.mojito.service.cache;

import com.box.l10n.mojito.service.DBUtils;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author garion
 */
public class DatabaseCacheTest extends ServiceTestBase {

    public final String TEST_CACHE_NAME = "testCacheInDb";
    public final String KEY = "key";
    public final String KEY2 = "key2";
    public final String VALUE = "value";
    public final String VALUE2 = "value2";

    public DatabaseCacheConfiguration databaseCacheConfiguration = new DatabaseCacheConfiguration();

    @Autowired
    DBUtils dbUtils;

    DatabaseCache databaseCache;

    @Transactional
    @Before
    public void setUp() {
        if (databaseCache == null) {
            databaseCacheConfiguration.setKeySerializationPair(new DefaultSerializationPair());
            databaseCacheConfiguration.setValueSerializationPair(new DefaultSerializationPair());
            databaseCache = new DatabaseCache(TEST_CACHE_NAME, databaseCacheConfiguration);
            databaseCache.clear();
        }
    }

    @Transactional
    @Test
    public void testPutGetWorksCorrectly() {
        databaseCache.put(KEY, VALUE);
        Assertions.assertEquals(VALUE, Objects.requireNonNull(databaseCache.get(KEY)).get());
    }

    @Transactional
    @Test
    public void testPutNullValueWorksCorrectly() {
        databaseCache.put(KEY, null);
        Assertions.assertNull(databaseCache.get(KEY));
    }

    @Transactional
    @Test
    public void testPutLookupWorksCorrectly() {
        databaseCache.put(KEY, VALUE);
        Assertions.assertEquals(VALUE, databaseCache.lookup(KEY));
    }

    @Transactional
    @Test
    public void testOverwriteEntryWorksCorrectly() {
        databaseCache.put(KEY, VALUE);
        Assertions.assertEquals(VALUE, databaseCache.lookup(KEY));

        databaseCache.put(KEY, VALUE2);
        Assertions.assertEquals(VALUE2, databaseCache.lookup(KEY));
    }

    @Transactional
    @Test
    public void testPutComplexObjectWorksCorrectly() {
        TestKey testKey = new TestKey();
        TestValue testValue = new TestValue();

        databaseCache.put(testKey, testValue);
        Assertions.assertEquals(testValue, databaseCache.lookup(testKey));
    }

    @Transactional
    @Test
    public void testEvictWorksCorrectly() {
        databaseCache.put(KEY, VALUE);
        databaseCache.put(KEY2, VALUE2);
        Assertions.assertEquals(VALUE, databaseCache.lookup(KEY));
        Assertions.assertEquals(VALUE2, databaseCache.lookup(KEY2));

        databaseCache.evict(KEY);

        Assertions.assertNull(databaseCache.lookup(KEY));
        Assertions.assertEquals(VALUE2, databaseCache.lookup(KEY2));
    }

    @Transactional
    @Test
    public void testClearWorksCorrectly() {
        databaseCache.put(KEY, VALUE);
        databaseCache.put(KEY2, VALUE2);
        Assertions.assertEquals(VALUE, databaseCache.lookup(KEY));
        Assertions.assertEquals(VALUE2, databaseCache.lookup(KEY2));

        databaseCache.clear();

        Assertions.assertNull(databaseCache.lookup(KEY));
        Assertions.assertNull(databaseCache.lookup(KEY2));
        Assertions.assertEquals(-0, ((ApplicationCacheRepository) databaseCache.getNativeCache()).findAll().size());
    }

    @Transactional
    @Test
    public void testEvictEntryOnDeserializationFailure() {
        SerializationPair<Object> defaultSerializationPair = new DefaultSerializationPair();
        SerializationPair<Object> mockSerializationPair = mock(DefaultSerializationPair.class);

        when(mockSerializationPair.write(ArgumentMatchers.any())).thenReturn(defaultSerializationPair.write(VALUE));

        when(mockSerializationPair.read(any())).thenThrow(new RuntimeException());
        databaseCacheConfiguration.setValueSerializationPair(mockSerializationPair);

        DatabaseCache databaseCacheWithMockedDeserializer = new DatabaseCache("databaseCacheWithMockedDeserializer", databaseCacheConfiguration);
        databaseCacheWithMockedDeserializer.put(KEY, VALUE);
        Assertions.assertNull(databaseCacheWithMockedDeserializer.lookup(KEY));
    }

    @Transactional
    @Test
    public void testExpiryDateLookup() throws InterruptedException {
        Duration ttl = Duration.ofSeconds(2);
        databaseCacheConfiguration.setTtl(ttl);
        DatabaseCache databaseCacheWith1SecondTTL = new DatabaseCache("databaseCacheWith1SecondTTL", databaseCacheConfiguration);

        databaseCacheWith1SecondTTL.put(KEY, VALUE);
        Assertions.assertEquals(VALUE, databaseCacheWith1SecondTTL.lookup(KEY));

        waitForCondition("wait for TTL expiry to take effect",
                () -> databaseCacheWith1SecondTTL.lookup(KEY) == null
        );
    }

    @Test
    public void testExpiryBasedEviction() throws InterruptedException {
        Duration ttl = Duration.ofMillis(1);
        databaseCacheConfiguration.setTtl(ttl);
        DatabaseCache databaseCacheWithAggressiveEviction = new DatabaseCache("databaseCacheWithAggressiveEviction", databaseCacheConfiguration);

        // This test is only supported on MySql
        Assume.assumeTrue(dbUtils.isMysql());

        putKeyValue(databaseCacheWithAggressiveEviction, KEY, VALUE);

        waitForCondition("wait for TTL expiry to take effect",
                () -> databaseCache.lookup(KEY) == null
        );
    }

    @Transactional
    private void putKeyValue(
            DatabaseCache databaseCacheWithAggressiveEviction,
            String key,
            String value) {

        databaseCacheWithAggressiveEviction.put(key, value);
    }

    static class TestKey implements Serializable {
        public String key = "testKey";
        public List<String> subValues = new ArrayList<>();

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestKey testKey = (TestKey) o;
            return com.google.common.base.Objects.equal(key, testKey.key) && com.google.common.base.Objects.equal(subValues, testKey.subValues);
        }

        @Override
        public int hashCode() {
            return com.google.common.base.Objects.hashCode(key, subValues);
        }
    }

    static class TestValue implements Serializable {
        public String value;
        public List<String> subValues = new ArrayList<>();

        public TestValue() {
            subValues.add("1");
            subValues.add("2");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestValue testValue = (TestValue) o;
            return com.google.common.base.Objects.equal(value, testValue.value) && com.google.common.base.Objects.equal(subValues, testValue.subValues);
        }

        @Override
        public int hashCode() {
            return com.google.common.base.Objects.hashCode(value, subValues);
        }
    }
}