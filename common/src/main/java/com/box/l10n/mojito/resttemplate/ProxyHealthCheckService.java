package com.box.l10n.mojito.resttemplate;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty("l10n.proxy.enabled")
public class ProxyHealthCheckService implements InitializingBean {

  private boolean isHealthy;

  @Autowired ResttemplateConfig restTemplateConfig;

  @Autowired ProxyConfig proxyConfig;

  @Override
  public void afterPropertiesSet() {
    RestTemplate restTemplate = buildRestTemplate();
    ProxyHealthChecker proxyHealthChecker = new ProxyHealthChecker();
    isHealthy = proxyHealthChecker.isProxyHealthy(restTemplate, restTemplateConfig, proxyConfig);
  }

  public boolean isProxyHealthy() {
    return isHealthy;
  }

  /***
   * This is needed because the default RestTemplate does not allow the caller
   * to set the Host header
   */
  private RestTemplate buildRestTemplate() {
    CloseableHttpClient httpClient =
        HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom().build()).build();
    return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
  }
}
