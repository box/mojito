package com.box.l10n.mojito.resttemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class ProxyHealthChecker {
  Logger logger = LoggerFactory.getLogger(ProxyHealthChecker.class);

  public boolean isProxyHealthy(
      RestTemplate restTemplate, ResttemplateConfig restTemplateConfig, ProxyConfig proxyConfig) {

    if (proxyConfig == null) {
      logger.warn("Proxy configuration was not provided. Falling back to directly accessing host");
      return false;
    }

    if (!proxyConfig.isValidConfiguration()) {
      logger.warn(
          "Proxy configuration is missing some required fields. Falling back to directly accessing host");
      return false;
    }

    String testUrl =
        UriComponentsBuilder.newInstance()
            .scheme(proxyConfig.getScheme())
            .host(proxyConfig.getHost())
            .port(proxyConfig.getPort())
            .path("login")
            .build()
            .toUriString();
    logger.debug("Checking if proxy is configured with URL '{}'", testUrl);
    HttpHeaders headers = new HttpHeaders();
    headers.set("Host", restTemplateConfig.getHost());
    logger.debug("With headers {}", headers);
    HttpEntity<String> httpEntity = new HttpEntity<>(null, headers);
    try {
      ResponseEntity<Void> response =
          restTemplate.exchange(testUrl, HttpMethod.GET, httpEntity, Void.class);
      logger.debug("Proxy login request response code {}", response.getStatusCode());
      return response.getStatusCode().is2xxSuccessful();
    } catch (Exception e) {
      logger.warn(
          "Proxy does not allow access to specified host. Falling back to directly accessing it",
          e);
      return false;
    }
  }
}
