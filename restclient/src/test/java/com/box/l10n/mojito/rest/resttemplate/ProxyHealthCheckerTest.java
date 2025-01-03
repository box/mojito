package com.box.l10n.mojito.rest.resttemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * @author byronantak
 */
class ProxyHealthCheckerTest {

  @Mock private RestTemplate restTemplate;

  ProxyHealthChecker proxyHealthChecker;

  ProxyConfig proxyConfig;

  @BeforeEach
  void init() {
    MockitoAnnotations.openMocks(this);
    proxyHealthChecker = new ProxyHealthChecker();
    proxyConfig = new ProxyConfig();
    proxyConfig.setHost("localhost");
    proxyConfig.setPort(19193);
    proxyConfig.setScheme("http");
  }

  @Test
  void shouldReturnFalseWhenProxyConfigIsNullOrIncomplete() {
    when(restTemplate.exchange(
            anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Void.class)))
        .thenReturn(new ResponseEntity<Void>(HttpStatus.OK));

    boolean isHealthy =
        proxyHealthChecker.isProxyHealthy(restTemplate, new ResttemplateConfig(), null);
    assertFalse(isHealthy);
    isHealthy =
        proxyHealthChecker.isProxyHealthy(
            restTemplate, new ResttemplateConfig(), new ProxyConfig());
    assertFalse(isHealthy);
  }

  @Test
  void shouldReturnTrueWhenProxyRequestRespondsWithA200() {
    when(restTemplate.exchange(
            anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Void.class)))
        .thenReturn(new ResponseEntity<Void>(HttpStatus.OK));

    boolean isHealthy =
        proxyHealthChecker.isProxyHealthy(restTemplate, new ResttemplateConfig(), proxyConfig);
    assertTrue(isHealthy);
  }

  @Test
  void shouldReturnFalseWhenProxyRequestRaisesException() {
    when(restTemplate.exchange(
            anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Void.class)))
        .thenThrow(new RestClientException("An error occurred"));

    boolean isHealthy =
        proxyHealthChecker.isProxyHealthy(restTemplate, new ResttemplateConfig(), proxyConfig);
    assertFalse(isHealthy);
  }

  @Test
  void shouldReturnFalseWhenProxyRequestReturnsBadStatusCode() {
    when(restTemplate.exchange(
            anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Void.class)))
        .thenReturn(new ResponseEntity<Void>(HttpStatus.NOT_FOUND));

    boolean isHealthy =
        proxyHealthChecker.isProxyHealthy(restTemplate, new ResttemplateConfig(), proxyConfig);
    assertFalse(isHealthy);
  }
}
