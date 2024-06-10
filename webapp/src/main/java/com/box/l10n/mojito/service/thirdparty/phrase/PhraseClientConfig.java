package com.box.l10n.mojito.service.thirdparty.phrase;

import com.phrase.client.ApiClient;
import com.phrase.client.auth.ApiKeyAuth;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PhraseClientConfig {

  @ConditionalOnProperty("l10n.phrase.client.token")
  @Bean
  public PhraseClient getPhraseClient(PhraseClientPropertiesConfig phraseClientPropertiesConfig) {

    ApiClient apiClient = new ApiClient();
    ApiKeyAuth authentication = (ApiKeyAuth) apiClient.getAuthentication("Token");
    authentication.setApiKey(phraseClientPropertiesConfig.getToken());
    authentication.setApiKeyPrefix("token");

    return new PhraseClient(apiClient);
  }
}
