package com.box.l10n.mojito;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Arrays;

/**
 *
 * @author jaurambault
 */
@Configuration
@EnableCaching(mode = AdviceMode.ASPECTJ)
public class CachingConfig extends CachingConfigurerSupport {

    @Bean
    @Override
    public CacheManager cacheManager() {
        Cache defaultCache = new ConcurrentMapCache("default");
        Cache localesCache = new ConcurrentMapCache("locales");

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(Arrays.asList(defaultCache, localesCache));

        return manager;
    }

    @Bean
    @Override
    public KeyGenerator keyGenerator() {
        return new CustomKeyGenerator();
    }
}
