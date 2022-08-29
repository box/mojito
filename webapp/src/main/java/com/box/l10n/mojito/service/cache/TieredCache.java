package com.box.l10n.mojito.service.cache;

import com.google.common.base.Preconditions;
import java.util.concurrent.Callable;
import javax.annotation.Nullable;
import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractValueAdaptingCache;

/**
 * Enables a tiered caching setup where items are stored in both caches and retrieval is done
 * preferentially from the tier 1 cache first and then on cache miss from tier 2.
 *
 * <p>Usually expected to be used with an ephemeral in-process memory cache as tier 1 and a
 * durable/persistent tier 2 cache.
 *
 * @author garion
 */
public class TieredCache extends AbstractValueAdaptingCache {

  private final String cacheName;
  private final Cache tier1Cache;
  private final Cache tier2Cache;

  public TieredCache(String cacheName, Cache tier1Cache, @Nullable Cache tier2Cache) {
    super(true);

    Preconditions.checkNotNull(cacheName);
    Preconditions.checkNotNull(tier1Cache);

    this.cacheName = cacheName;
    this.tier1Cache = tier1Cache;
    this.tier2Cache = tier2Cache;
  }

  @Override
  public String getName() {
    return cacheName;
  }

  @Override
  public Object getNativeCache() {
    return tier1Cache;
  }

  @Override
  public <T> T get(Object key, Callable<T> valueLoader) {
    //noinspection unchecked
    T cacheEntry = (T) lookup(key);
    if (cacheEntry == null) {
      try {
        cacheEntry = valueLoader.call();
        put(key, cacheEntry);
      } catch (Throwable ex) {
        throw new ValueRetrievalException(key, valueLoader, ex);
      }
    }

    return cacheEntry;
  }

  @Override
  public void put(Object key, Object value) {
    tier1Cache.put(key, value);

    if (tier2Cache != null) {
      tier2Cache.put(key, value);
    }
  }

  @Override
  public void evict(Object o) {
    tier1Cache.evict(o);

    if (tier2Cache != null) {
      tier2Cache.evict(o);
    }
  }

  @Override
  public void clear() {
    tier1Cache.clear();

    if (tier2Cache != null) {
      tier2Cache.clear();
    }
  }

  @Override
  protected Object lookup(Object key) {
    Object cacheHit = null;
    ValueWrapper tier1CacheValueWrapper = tier1Cache.get(key);

    if (tier1CacheValueWrapper != null) {
      cacheHit = tier1CacheValueWrapper.get();
    } else {
      // In case of a tier 1 cache miss, retrieve from tier 2 and populate tier 1
      if (tier2Cache != null) {
        ValueWrapper tier2CacheValueWrapper = tier2Cache.get(key);
        if (tier2CacheValueWrapper != null) {
          cacheHit = tier2CacheValueWrapper.get();
          tier1Cache.put(key, cacheHit);
        }
      }
    }

    return cacheHit;
  }
}
