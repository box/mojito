package com.box.l10n.mojito.rest.textunit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class SearchTextUnitsHybridConfig {

  private final SearchTextUnitsHybridProperties searchTextUnitsHybridProperties;

  public SearchTextUnitsHybridConfig(
      SearchTextUnitsHybridProperties searchTextUnitsHybridProperties) {
    this.searchTextUnitsHybridProperties = searchTextUnitsHybridProperties;
  }

  @Bean(name = "searchTextUnitsHybridExecutor")
  public ThreadPoolTaskExecutor searchTextUnitsHybridExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(searchTextUnitsHybridProperties.pool().corePoolSize());
    executor.setMaxPoolSize(searchTextUnitsHybridProperties.pool().maxPoolSize());
    executor.setQueueCapacity(searchTextUnitsHybridProperties.pool().queueCapacity());
    executor.setThreadNamePrefix(searchTextUnitsHybridProperties.pool().threadNamePrefix() + "-");
    executor.initialize();
    return executor;
  }
}
