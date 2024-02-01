package com.box.l10n.mojito;

import com.box.l10n.mojito.service.cache.DatabaseCache;
import com.box.l10n.mojito.service.cache.DatabaseCacheConfiguration;
import com.box.l10n.mojito.service.cache.TieredCache;
import com.box.l10n.mojito.service.machinetranslation.MTServiceCacheConfiguration;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author jaurambault
 */
@Configuration
@EnableCaching(mode = AdviceMode.ASPECTJ)
public class CachingConfig extends CachingConfigurerSupport {

  final MTServiceCacheConfiguration mtServiceCacheConfiguration;

  public CachingConfig(MTServiceCacheConfiguration mtServiceCacheConfiguration) {
    this.mtServiceCacheConfiguration = mtServiceCacheConfiguration;
  }

  @Bean
  @Override
  public CacheManager cacheManager() {
    Cache defaultCache = new ConcurrentMapCache(CacheType.Names.DEFAULT);
    Cache localesCache = new ConcurrentMapCache(CacheType.Names.LOCALES);
    Cache pluralForm = new ConcurrentMapCache(CacheType.Names.PLURAL_FORMS);

    TieredCache machineTranslationTieredCache = getMachineTranslationCache();

    SimpleCacheManager manager = new SimpleCacheManager();
    manager.setCaches(
        Arrays.asList(defaultCache, localesCache, pluralForm, machineTranslationTieredCache));

    return manager;
  }

  private TieredCache getMachineTranslationCache() {
    com.github.benmanes.caffeine.cache.@NonNull Cache<Object, Object>
        machineTranslationCaffeineCache =
            Caffeine.newBuilder()
                .expireAfterWrite(
                    mtServiceCacheConfiguration.getInMemory().getTtl().toMillis(),
                    TimeUnit.MILLISECONDS)
                .maximumSize(mtServiceCacheConfiguration.getInMemory().getMaximumSize())
                .build();

    CaffeineCache machineTranslationMemoryCache =
        new CaffeineCache("machineTranslationInMemory", machineTranslationCaffeineCache);

    DatabaseCache machineTranslationDatabaseCache = null;

    if (mtServiceCacheConfiguration.getDatabase().isEnabled()) {
      DatabaseCacheConfiguration mtDbCacheConfiguration = new DatabaseCacheConfiguration();
      mtDbCacheConfiguration.setTtl(mtServiceCacheConfiguration.getDatabase().getTtl());
      mtDbCacheConfiguration.setEvictEntryOnDeserializationFailure(
          mtServiceCacheConfiguration.getDatabase().isEvictEntryOnDeserializationFailure());

      machineTranslationDatabaseCache =
          new DatabaseCache("machineTranslationInDb", mtDbCacheConfiguration);
    }

    return new TieredCache(
        CacheType.Names.MACHINE_TRANSLATION,
        machineTranslationMemoryCache,
        machineTranslationDatabaseCache);
  }

  @Bean
  @Override
  public KeyGenerator keyGenerator() {
    return new CustomKeyGenerator();
  }
}
