package com.box.l10n.mojito.service.evolve;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

@ConditionalOnProperty("l10n.evolve.url")
@Configuration
public class EvolveConfiguration {

  static Logger resttemplateLogger =
      LoggerFactory.getLogger(EvolveConfiguration.class.getPackage().getName() + ".Resttemplate");

  @Autowired EvolveConfigurationProperties evolveConfigurationProperties;

  @Autowired private EvolveOAuthClient evolveOAuthClient;

  @Bean
  EvolveClient evolveClient() {
    return new EvolveClient(
        evolveRestTemplate(),
        this.evolveConfigurationProperties.getApiPath(),
        this.evolveConfigurationProperties.getMaxRetries(),
        this.evolveConfigurationProperties.getRetryMinBackoffSecs(),
        this.evolveConfigurationProperties.getRetryMaxBackoffSecs());
  }

  RestTemplate evolveRestTemplate() {
    RestTemplate restTemplate = new RestTemplate();

    restTemplate.setUriTemplateHandler(
        new DefaultUriBuilderFactory(evolveConfigurationProperties.getUrl()));

    restTemplate
        .getInterceptors()
        .add(
            (request, body, execution) -> {
              request
                  .getHeaders()
                  .add(
                      HttpHeaders.AUTHORIZATION,
                      "Bearer " + this.evolveOAuthClient.getAccessToken());
              return execution.execute(request, body);
            });

    restTemplate.setErrorHandler(
        new DefaultResponseErrorHandler() {
          @Override
          public void handleError(ClientHttpResponse response) throws IOException {
            try {
              super.handleError(response);
            } catch (HttpServerErrorException | HttpClientErrorException e) {
              resttemplateLogger.debug(e.getResponseBodyAsString());
              throw e;
            }
          }
        });

    return restTemplate;
  }
}
