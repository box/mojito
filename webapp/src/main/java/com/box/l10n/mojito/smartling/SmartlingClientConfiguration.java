package com.box.l10n.mojito.smartling;

import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

/**
 * @author jaurambault
 */
@Configuration
@ConfigurationProperties("l10n.smartling")
public class SmartlingClientConfiguration {
  String accessTokenUri = "https://api.smartling.com/auth-api/v2/authenticate";
  String refreshTokenUri = "https://api.smartling.com/auth-api/v2/authenticate/refresh";

  String clientID;
  String clientSecret;
  int retryMaxAttempts = 10;
  int retryMinDurationSeconds = 1;
  int retryMaxBackoffDurationSeconds = 60;

  @ConditionalOnProperty("l10n.smartling.clientID")
  @Bean
  public SmartlingClient getSmartlingClient(
      SmartlingOAuth2TokenService smartlingOAuth2TokenService) {
    RetryBackoffSpec retryConfiguration =
        Retry.backoff(getRetryMaxAttempts(), Duration.ofSeconds(getRetryMinDurationSeconds()))
            .maxBackoff(Duration.ofSeconds(getRetryMaxBackoffDurationSeconds()));

    return new SmartlingClient(smartlingOAuth2TokenService, retryConfiguration);
  }

  @ConditionalOnProperty("l10n.smartling.clientID")
  @Bean
  public SmartlingOAuth2TokenService smartlingOAuth2TokenService() {
    return new SmartlingOAuth2TokenService(clientID, clientSecret, accessTokenUri, refreshTokenUri);
  }

  public String getAccessTokenUri() {
    return accessTokenUri;
  }

  public void setAccessTokenUri(String accessTokenUri) {
    this.accessTokenUri = accessTokenUri;
  }

  public String getRefreshTokenUri() {
    return refreshTokenUri;
  }

  public void setRefreshTokenUri(String refreshTokenUri) {
    this.refreshTokenUri = refreshTokenUri;
  }

  public String getClientID() {
    return clientID;
  }

  public void setClientID(String clientID) {
    this.clientID = clientID;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }

  public int getRetryMaxAttempts() {
    return retryMaxAttempts;
  }

  public void setRetryMaxAttempts(int retryMaxAttempts) {
    this.retryMaxAttempts = retryMaxAttempts;
  }

  public int getRetryMinDurationSeconds() {
    return retryMinDurationSeconds;
  }

  public void setRetryMinDurationSeconds(int retryMinDurationSeconds) {
    this.retryMinDurationSeconds = retryMinDurationSeconds;
  }

  public int getRetryMaxBackoffDurationSeconds() {
    return retryMaxBackoffDurationSeconds;
  }

  public void setRetryMaxBackoffDurationSeconds(int retryMaxBackoffDurationSeconds) {
    this.retryMaxBackoffDurationSeconds = retryMaxBackoffDurationSeconds;
  }
}
