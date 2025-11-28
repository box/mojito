package com.box.l10n.mojito.rest.textunit;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "l10n.textunitws.search.hybrid")
public record SearchTextUnitsHybridProperties(
    @DefaultValue("PT1S") Duration convertToAsyncAfter,
    @DefaultValue("PT60S") Duration recommendedPollingDuration,
    @DefaultValue Pool pool) {

  record Pool(
      @DefaultValue("4") int corePoolSize,
      @DefaultValue("8") int maxPoolSize,
      @DefaultValue("100") int queueCapacity,
      @DefaultValue("textunit-search") String threadNamePrefix) {}
}
