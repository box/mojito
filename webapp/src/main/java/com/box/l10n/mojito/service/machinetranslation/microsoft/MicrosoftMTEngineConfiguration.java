package com.box.l10n.mojito.service.machinetranslation.microsoft;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration settings for the Microsoft Machine Translation client. Only used when
 * "l10n.mt.impl" has the "MicrosoftMTEngine" value and the subscription key is provided in the
 * "l10n.microsoft-mt.subscriptionKey" property.
 *
 * @author garion
 */
@Component
@ConfigurationProperties("l10n.microsoft-mt")
public class MicrosoftMTEngineConfiguration {
  private String baseApiUrl = "https://api.cognitive.microsofttranslator.com/";
  private String subscriptionKey;
  private String subscriptionRegion = "global";

  public String getBaseApiUrl() {
    return baseApiUrl;
  }

  public void setBaseApiUrl(String baseApiUrl) {
    this.baseApiUrl = baseApiUrl;
  }

  public String getSubscriptionKey() {
    return subscriptionKey;
  }

  public void setSubscriptionKey(String subscriptionKey) {
    this.subscriptionKey = subscriptionKey;
  }

  public String getSubscriptionRegion() {
    return subscriptionRegion;
  }

  public void setSubscriptionRegion(String subscriptionRegion) {
    this.subscriptionRegion = subscriptionRegion;
  }
}
