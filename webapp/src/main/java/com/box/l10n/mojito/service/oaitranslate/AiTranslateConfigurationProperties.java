package com.box.l10n.mojito.service.oaitranslate;

import com.box.l10n.mojito.quartz.QuartzSchedulerManager;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n.ai-translate")
public class AiTranslateConfigurationProperties {
  String openaiClientToken;
  String schedulerName = QuartzSchedulerManager.DEFAULT_SCHEDULER_NAME;

  public String getOpenaiClientToken() {
    return openaiClientToken;
  }

  public void setOpenaiClientToken(String openaiClientToken) {
    this.openaiClientToken = openaiClientToken;
  }

  public String getSchedulerName() {
    return schedulerName;
  }

  public void setSchedulerName(String schedulerName) {
    this.schedulerName = schedulerName;
  }
}
