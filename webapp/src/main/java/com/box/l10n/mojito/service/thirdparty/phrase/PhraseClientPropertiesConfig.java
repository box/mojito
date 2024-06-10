package com.box.l10n.mojito.service.thirdparty.phrase;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n.phrase.client")
public class PhraseClientPropertiesConfig {

  String token;

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }
}
