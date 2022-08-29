package com.box.l10n.mojito.smartling;

import com.box.l10n.mojito.utils.RestTemplateUtils;
import com.fasterxml.jackson.databind.DeserializationFeature;
import java.io.IOException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.DefaultUriBuilderFactory;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

/** @author jaurambault */
@Configuration
@EnableOAuth2Client
@ConfigurationProperties("l10n.smartling")
public class SmartlingClientConfiguration {

  static Logger resttemplateLogger =
      LoggerFactory.getLogger(
          SmartlingClientConfiguration.class.getPackage().getName() + ".Resttemplate");

  String baseUri = "https://api.smartling.com/";
  String accessTokenUri = "https://api.smartling.com/auth-api/v2/authenticate";
  String refreshTokenUri = "https://api.smartling.com/auth-api/v2/authenticate/refresh";

  String clientID;
  String clientSecret;
  int retryMaxAttempts = 10;
  int retryMinDurationSeconds = 1;
  int retryMaxBackoffDurationSeconds = 60;

  @ConditionalOnProperty("l10n.smartling.clientID")
  @Bean
  public SmartlingClient getSmartlingClient() {
    RetryBackoffSpec retryConfiguration =
        Retry.backoff(getRetryMaxAttempts(), Duration.ofSeconds(getRetryMinDurationSeconds()))
            .maxBackoff(Duration.ofSeconds(getRetryMaxBackoffDurationSeconds()));

    return new SmartlingClient(smartlingRestTemplate(), retryConfiguration);
  }

  @Bean
  public OAuth2ProtectedResourceDetails smartling() {
    SmartlingOAuth2ProtectedResourceDetails details = new SmartlingOAuth2ProtectedResourceDetails();
    details.setId("Smartling");
    details.setGrantType("smartling");
    details.setClientId(clientID);
    details.setClientSecret(clientSecret);
    details.setAccessTokenUri(accessTokenUri);
    details.setRefreshUri(refreshTokenUri);
    return details;
  }

  public OAuth2RestTemplate smartlingRestTemplate() {
    OAuth2RestTemplate oAuth2RestTemplate =
        new OAuth2RestTemplate(smartling(), new DefaultOAuth2ClientContext());

    RestTemplateUtils restTemplateUtils = new RestTemplateUtils();
    restTemplateUtils.enableFeature(oAuth2RestTemplate, DeserializationFeature.UNWRAP_ROOT_VALUE);

    oAuth2RestTemplate.setAccessTokenProvider(new SmartlingAuthorizationCodeAccessTokenProvider());
    oAuth2RestTemplate.setRetryBadAccessTokens(true);

    DefaultUriBuilderFactory defaultUriTemplateHandler = new DefaultUriBuilderFactory(baseUri);
    oAuth2RestTemplate.setUriTemplateHandler(defaultUriTemplateHandler);

    oAuth2RestTemplate.setErrorHandler(
        new DefaultResponseErrorHandler() {
          @Override
          public void handleError(ClientHttpResponse response) throws IOException {
            try {
              super.handleError(response);
            } catch (HttpClientErrorException e) {
              if (resttemplateLogger.isDebugEnabled()) {
                resttemplateLogger.debug(e.getResponseBodyAsString());
              }
              throw e;
            }
          }
        });

    return oAuth2RestTemplate;
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
