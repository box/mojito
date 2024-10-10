package com.box.l10n.mojito.pagerduty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "l10n.pagerduty.retry")
public class PagerDutyRetryConfiguration {
  private int maxRetries = 3;
  private long minBackOffDelay = 500;
  private long maxBackOffDelay = 5000;

  public int getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(int maxRetries) {
    this.maxRetries = maxRetries;
  }

  public long getMinBackOffDelay() {
    return minBackOffDelay;
  }

  public void setMinBackOffDelay(long minBackOffDelay) {
    this.minBackOffDelay = minBackOffDelay;
  }

  public long getMaxBackOffDelay() {
    return maxBackOffDelay;
  }

  public void setMaxBackOffDelay(long maxBackOffDelay) {
    this.maxBackOffDelay = maxBackOffDelay;
  }
}
