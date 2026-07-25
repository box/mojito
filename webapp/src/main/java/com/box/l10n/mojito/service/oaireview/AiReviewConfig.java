package com.box.l10n.mojito.service.oaireview;

import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.openai.OpenAIClient;
import com.box.l10n.mojito.openai.OpenAIClientPool;
import com.box.l10n.mojito.openai.OpenAIProxyConfig;
import com.box.l10n.mojito.proxy.ResolvedWebProxy;
import com.box.l10n.mojito.proxy.WebProxyConfigurationProperties;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

@Configuration
public class AiReviewConfig {

  AiReviewConfigurationProperties aiReviewConfigurationProperties;
  WebProxyConfigurationProperties webProxyConfigurationProperties;

  public AiReviewConfig(
      AiReviewConfigurationProperties aiReviewConfigurationProperties,
      WebProxyConfigurationProperties webProxyConfigurationProperties) {
    this.aiReviewConfigurationProperties = aiReviewConfigurationProperties;
    this.webProxyConfigurationProperties = webProxyConfigurationProperties;
  }

  @Bean("openAIClientReview")
  OpenAIClient openAIClient() {
    String openaiClientToken = aiReviewConfigurationProperties.getOpenaiClientToken();
    if (openaiClientToken == null) {
      return null;
    }
    return OpenAIClient.builder().apiKey(openaiClientToken).proxyConfig(getProxyConfig()).build();
  }

  @Bean("openAIClientPoolReview")
  OpenAIClientPool openAIClientPool() {
    String openaiClientToken = aiReviewConfigurationProperties.getOpenaiClientToken();
    if (openaiClientToken == null) {
      return null;
    }
    return new OpenAIClientPool(
        10, 10, 5, aiReviewConfigurationProperties.getOpenaiClientToken(), getProxyConfig());
  }

  OpenAIProxyConfig getProxyConfig() {
    ResolvedWebProxy resolved =
        webProxyConfigurationProperties.resolve(
            aiReviewConfigurationProperties.getProxyHost(),
            aiReviewConfigurationProperties.getProxyPort(),
            aiReviewConfigurationProperties.getProxyUser(),
            aiReviewConfigurationProperties.getProxyPassword());
    return OpenAIProxyConfig.of(
        resolved.host(),
        resolved.port(),
        resolved.user(),
        resolved.password(),
        webProxyConfigurationProperties.isAllowBasicTunneling());
  }

  @Bean("objectMapperReview")
  ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    AiReviewService.configureObjectMapper(objectMapper);
    return objectMapper;
  }

  @Bean("retryBackoffSpecReview")
  RetryBackoffSpec retryBackoffSpec() {
    return Retry.backoff(5, Duration.ofMillis(500)).maxBackoff(Duration.ofSeconds(5));
  }
}
