package com.box.l10n.mojito.quartz.multi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("l10n.org.multi-quartz")
@ConditionalOnProperty(name = "l10n.org.multi-quartz.enabled", havingValue = "true")
public class QuartzMultiSchedulerConfigurationProperties {

  Map<String, SchedulerConfigurationProperties> schedulers;

  List<SchedulerConfigurationProperties> schedulerConfigurationProperties = new ArrayList<>();

  public Map<String, SchedulerConfigurationProperties> getSchedulers() {
    return schedulers;
  }

  public void setSchedulers(Map<String, SchedulerConfigurationProperties> schedulers) {
    this.schedulers = schedulers;
  }
}

class SchedulerConfigurationProperties {

  private Map<String, String> quartz = new HashMap<>();

  public Map<String, String> getQuartz() {
    return quartz;
  }

  public void setQuartz(Map<String, String> quartz) {
    this.quartz = quartz;
  }
}
