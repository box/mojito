package com.box.l10n.mojito.service.oaitranslate;

import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.openai.OpenAIClient;
import com.box.l10n.mojito.openai.OpenAIClientPool;
import com.box.l10n.mojito.openai.OpenAIProxyConfig;
import com.box.l10n.mojito.proxy.ResolvedWebProxy;
import com.box.l10n.mojito.proxy.WebProxyConfigurationProperties;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

@Configuration
public class AiTranslateConfig {

  AiTranslateConfigurationProperties aiTranslateConfigurationProperties;
  WebProxyConfigurationProperties webProxyConfigurationProperties;

  public AiTranslateConfig(
      AiTranslateConfigurationProperties aiTranslateConfigurationProperties,
      WebProxyConfigurationProperties webProxyConfigurationProperties) {
    this.aiTranslateConfigurationProperties = aiTranslateConfigurationProperties;
    this.webProxyConfigurationProperties = webProxyConfigurationProperties;
  }

  @Bean
  @Qualifier("AiTranslate")
  OpenAIClient openAIClient() {
    String openaiClientToken = aiTranslateConfigurationProperties.getOpenaiClientToken();
    if (openaiClientToken == null) {
      return null;
    }
    return OpenAIClient.builder()
        .apiKey(openaiClientToken)
        .proxyConfig(getProxyConfig())
        .build();
  }

  @Bean
  @Qualifier("AiTranslate")
  OpenAIClientPool openAIClientPool() {
    String openaiClientToken = aiTranslateConfigurationProperties.getOpenaiClientToken();
    if (openaiClientToken == null) {
      return null;
    }
    return new OpenAIClientPool(
        20,
        100,
        1,
        aiTranslateConfigurationProperties.getOpenaiClientToken(),
        getProxyConfig());
  }

  OpenAIProxyConfig getProxyConfig() {
    ResolvedWebProxy resolved =
        webProxyConfigurationProperties.resolve(
            aiTranslateConfigurationProperties.getProxyHost(),
            aiTranslateConfigurationProperties.getProxyPort(),
            aiTranslateConfigurationProperties.getProxyUser(),
            aiTranslateConfigurationProperties.getProxyPassword());
    return OpenAIProxyConfig.of(
        resolved.host(),
        resolved.port(),
        resolved.user(),
        resolved.password(),
        webProxyConfigurationProperties.isAllowBasicTunneling());
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
