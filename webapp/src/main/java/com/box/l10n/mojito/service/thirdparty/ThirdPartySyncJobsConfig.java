package com.box.l10n.mojito.service.thirdparty;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n")
public class ThirdPartySyncJobsConfig {

  Map<String, ThirdPartySyncJobConfig> thirdPartySyncJobs = new HashMap<>();

  public Map<String, ThirdPartySyncJobConfig> getThirdPartySyncJobs() {
    return thirdPartySyncJobs;
  }

  public void setThirdPartySyncJobs(Map<String, ThirdPartySyncJobConfig> thirdPartySyncJobs) {
    this.thirdPartySyncJobs = thirdPartySyncJobs;
  }
}
