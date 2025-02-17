package com.box.l10n.mojito.service.oaireview;

import com.box.l10n.mojito.quartz.QuartzSchedulerManager;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n.ai-review")
public class AiReviewConfigurationProperties {
  String openaiClientToken;
  String schedulerName = QuartzSchedulerManager.DEFAULT_SCHEDULER_NAME;
  String modelName = "gpt-4o-2024-08-06";

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

  public String getModelName() {
    return modelName;
  }

  public void setModelName(String modelName) {
    this.modelName = modelName;
  }
}
