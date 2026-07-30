package com.box.l10n.mojito.service.oaitranslate;

import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.openai.OpenAIClient;
import com.box.l10n.mojito.openai.OpenAIClientPool;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

@Configuration
public class AiTranslateConfig {

  AiTranslateConfigurationProperties aiTranslateConfigurationProperties;

  public AiTranslateConfig(AiTranslateConfigurationProperties aiTranslateConfigurationProperties) {
    this.aiTranslateConfigurationProperties = aiTranslateConfigurationProperties;
  }

  @Bean
  @Qualifier("AiTranslate")
  OpenAIClient openAIClient() {
    String openaiClientToken = aiTranslateConfigurationProperties.getOpenaiClientToken();
    if (openaiClientToken == null) {
      return null;
    }
    return new OpenAIClient.Builder().apiKey(openaiClientToken).build();
  }

  @Bean
  @Qualifier("AiTranslate")
  OpenAIClientPool openAIClientPool() {
    String openaiClientToken = aiTranslateConfigurationProperties.getOpenaiClientToken();
    if (openaiClientToken == null) {
      return null;
    }
    return new OpenAIClientPool(
        20, 100, 1, aiTranslateConfigurationProperties.getOpenaiClientToken());
  }

  @Bean
  @Qualifier("AiTranslate")
  ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    AiTranslateService.configureObjectMapper(objectMapper);
    return objectMapper;
  }

  @Bean
  @Qualifier("AiTranslate")
  RetryBackoffSpec retryBackoffSpec() {
    return Retry.backoff(5, Duration.ofMillis(500)).maxBackoff(Duration.ofSeconds(5));
  }
}
