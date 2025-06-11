package com.box.l10n.mojito.service.blobstorage.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n.redis.client-refresher-pool")
public class ScheduledThreadPoolConfigProperties {
  private int poolSize = 1;

  private int periodInMinutes = 10;

  public int getPoolSize() {
    return poolSize;
  }

  public void setPoolSize(int poolSize) {
    this.poolSize = poolSize;
  }

  public int getPeriodInMinutes() {
    return periodInMinutes;
  }

  public void setPeriodInMinutes(int periodInMinutes) {
    this.periodInMinutes = periodInMinutes;
  }
}
