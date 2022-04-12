package com.box.l10n.mojito.service.cache;

import com.google.common.base.Preconditions;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * This class enables custom configurations for a database-backed cache.
 * By default, no TTL is configured (an expiry date isn't writen for entries).
 * Cache evictions is done by {@Link DatabaseCacheEvictionJob}.
 * The serializer and deserializers for the keys and values are also configurable.
 *
 * @author garion
 */
public class DatabaseCacheConfiguration {

    @DurationUnit(ChronoUnit.SECONDS)
    private Duration ttl;

    private boolean evictEntryOnDeserializationFailure;

    private SerializationPair<Object> keySerializationPair;
    private SerializationPair<Object> valueSerializationPair;

    public DatabaseCacheConfiguration() {
        this.ttl = Duration.ZERO;
        this.evictEntryOnDeserializationFailure = true;
        this.keySerializationPair = new DefaultSerializationPair();
        this.valueSerializationPair = new DefaultSerializationPair();
    }

    public void setTtl(Duration ttl) {
        Preconditions.checkNotNull(ttl);
        this.ttl = ttl;
    }

    public Duration getTtl() {
        return ttl;
    }

    public SerializationPair<Object> getKeySerializationPair() {
        return keySerializationPair;
    }

    public void setKeySerializationPair(SerializationPair<Object> keySerializationPair) {
        Preconditions.checkNotNull(keySerializationPair);
        this.keySerializationPair = keySerializationPair;
    }

    public SerializationPair<Object> getValueSerializationPair() {
        return valueSerializationPair;
    }

    @SuppressWarnings("unchecked")
    public void setValueSerializationPair(SerializationPair<?> valueSerializationPair) {
        Preconditions.checkNotNull(valueSerializationPair);
        this.valueSerializationPair = (SerializationPair<Object>) valueSerializationPair;
    }

    public boolean isEvictEntryOnDeserializationFailure() {
        return evictEntryOnDeserializationFailure;
    }

    public void setEvictEntryOnDeserializationFailure(boolean evictEntryOnDeserializationFailure) {
        this.evictEntryOnDeserializationFailure = evictEntryOnDeserializationFailure;
    }
}
