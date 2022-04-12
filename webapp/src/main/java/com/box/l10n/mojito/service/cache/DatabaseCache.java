package com.box.l10n.mojito.service.cache;

import com.box.l10n.mojito.entity.ApplicationCache;
import com.box.l10n.mojito.entity.ApplicationCacheType;
import com.google.common.base.Preconditions;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.cache.support.NullValue;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.annotation.Nullable;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Callable;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implements a cache that is stored in the database.
 * <p>
 * The key objects are serialized and an MD5 of the serialized result is used, together with the cache type, as the
 * identifying key in the database.
 * The value objects are serialized/deserialized to byte arrays and back.
 *
 * @author garion
 */
@Configurable
public class DatabaseCache extends AbstractValueAdaptingCache {
    /**
     * logger
     */
    static Logger logger = getLogger(DatabaseCache.class);

    private static final byte[] BINARY_NULL_VALUE = (new SerializingConverter()).convert(NullValue.INSTANCE);

    @Autowired
    ApplicationCacheUpdaterService applicationCacheUpdaterService;

    @Autowired
    ApplicationCacheTypeRepository applicationCacheTypeRepository;

    @Autowired
    ApplicationCacheRepository applicationCacheRepository;

    private final String cacheName;
    private final DatabaseCacheConfiguration cacheConfig;

    private ApplicationCacheType cacheType;

    public DatabaseCache(String cacheName, DatabaseCacheConfiguration cacheConfig) {
        super(true);

        Preconditions.checkNotNull(cacheName);
        Preconditions.checkNotNull(cacheConfig);

        this.cacheName = cacheName;
        this.cacheConfig = cacheConfig;

        logger.debug("DatabaseCache with name: {} and TTL: {} created.", cacheName, cacheConfig.getTtl());
    }

    @Override
    public Object lookup(Object key) {
        checkCacheTypeConfigured();

        Optional<ApplicationCache> maybeCacheResult = applicationCacheRepository.findByIdAndNotExpired(this.cacheType, getCacheKeyMD5(key));

        if (maybeCacheResult.isPresent()) {
            byte[] bytes = maybeCacheResult.get().getValue();

            // Permanently evict cache entries that cause deserialization failures (e.g.: due to breaking changes in
            // target class structure).
            try {
                Object deserializedValue = deserializeCacheValue(bytes);
                return this.fromStoreValue(deserializedValue);
            } catch (Exception e) {
                logger.error(String.format("Could not deserialize cache entry. Problematic serialized value: %s . Exception: ", Arrays.toString(bytes)), e);

                if (cacheConfig.isEvictEntryOnDeserializationFailure()) {
                    logger.warn(String.format("The entry failing deserialization will be evicted permanently. Problematic serialized value: %s ", Arrays.toString(bytes)));
                    evict(key);
                    return null;
                }

                throw e;
            }
        }

        return null;
    }

    @Override
    public String getName() {
        return this.cacheName;
    }

    @Override
    public Object getNativeCache() {
        return applicationCacheRepository;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        //noinspection unchecked
        T cacheEntry = (T) lookup(key);
        if (cacheEntry == null) {
            try {
                cacheEntry = valueLoader.call();
                put(key, serializeCacheValue(cacheEntry));
            } catch (Throwable ex) {
                throw new ValueRetrievalException(key, valueLoader, ex);
            }
        }

        return cacheEntry;
    }

    @Transactional
    @Override
    public void put(Object key, Object value) {
        checkCacheTypeConfigured();

        long ttlInSeconds = cacheConfig.getTtl().get(ChronoUnit.SECONDS);
        if (ttlInSeconds <= 0) {
            applicationCacheUpdaterService.upsertNoExpiryDate(
                    cacheType.getId(),
                    getCacheKeyMD5(key),
                    serializeCacheValue(value));
        } else {
            applicationCacheUpdaterService.upsertWithTTL(
                    cacheType.getId(),
                    getCacheKeyMD5(key),
                    serializeCacheValue(value),
                    ttlInSeconds);
        }
    }

    @Transactional
    @Override
    public void evict(Object key) {
        checkCacheTypeConfigured();
        applicationCacheRepository.deleteByApplicationCacheTypeAndKeyMD5(
                cacheType, getCacheKeyMD5(key)
        );
    }

    @Transactional
    @Override
    public void clear() {
        checkCacheTypeConfigured();
        applicationCacheRepository.clearCache(cacheType.getId());
    }

    private void checkCacheTypeConfigured() {
        if (this.cacheType == null) {
            this.cacheType = getOrRegisterApplicationCacheType(getName());
        }
    }

    private byte[] serializeCacheKey(Object cacheKey) {
        return cacheConfig.getKeySerializationPair().write(cacheKey);
    }

    private byte[] serializeCacheValue(Object value) {
        if (isAllowNullValues() && (value == null || value instanceof NullValue)) {
            return BINARY_NULL_VALUE;
        }

        return cacheConfig.getValueSerializationPair().write(value);
    }

    @Nullable
    private Object deserializeCacheValue(byte[] value) {
        if (isAllowNullValues() && ObjectUtils.nullSafeEquals(value, BINARY_NULL_VALUE)) {
            return NullValue.INSTANCE;
        }

        return cacheConfig.getValueSerializationPair().read(value);
    }

    private String getCacheKeyMD5(Object key) {
        byte[] serializedKey = serializeCacheKey(key);
        return DigestUtils.md5Hex(serializedKey);
    }

    /**
     * Auto-register the cache in the DB if it doesn't already exist.
     */
    @Transactional
    private ApplicationCacheType getOrRegisterApplicationCacheType(String name) {
        ApplicationCacheType dbCacheType;
        dbCacheType = applicationCacheTypeRepository.findByName(name);

        if (dbCacheType == null) {
            dbCacheType = applicationCacheTypeRepository.save(new ApplicationCacheType(name));
        }

        return dbCacheType;
    }
}
