package com.box.l10n.mojito.rest.resttemplate;

import java.io.IOException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@ConditionalOnProperty("l10n.proxy.enabled")
public class ProxyOutboundRequestInterceptor implements ClientHttpRequestInterceptor {

  Logger logger = LoggerFactory.getLogger(ProxyOutboundRequestInterceptor.class);

  @Autowired ResttemplateConfig restTemplateConfig;

  @Autowired(required = false)
  ProxyHealthCheckService proxyHealthCheckService;

  @Autowired(required = false)
  ProxyConfig proxyConfig;

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    if (proxyConfig == null || !proxyConfig.isValidConfiguration()) {
      return execution.execute(request, body);
    }

    if (proxyHealthCheckService.isProxyHealthy()) {
      // To prevent adding extra layers, when the proxied request fails and is retried
      if (request.getURI().getHost().equals(proxyConfig.getHost())) {
        logger.debug("Proxy has already been configured for request");
        return execution.execute(request, body);
      }

      logger.debug("Configuring request via proxy");
      String rawPath = request.getURI().getRawPath();
      String rawQuery = request.getURI().getRawQuery();
      URI proxyUri =
          UriComponentsBuilder.newInstance()
              .scheme(proxyConfig.getScheme())
              .host(proxyConfig.getHost())
              .port(proxyConfig.getPort())
              .path(rawPath)
              .query(rawQuery)
              .build()
              .toUri();

      HttpRequest modifiedRequest = buildProxiedRequest(request, proxyUri);
      logger.debug(
          "Modified Proxy Request. Method: {}. Uri: {}. Headers: {}",
          modifiedRequest.getMethod(),
          modifiedRequest.getURI(),
          modifiedRequest.getHeaders());
      return execution.execute(modifiedRequest, body);
    }

    logger.debug("Proxy is not configured for request");
    return execution.execute(request, body);
  }

  private HttpRequest buildProxiedRequest(HttpRequest request, URI proxyUri) {
    return new HttpRequest() {
      @Override
      public HttpMethod getMethod() {
        return request.getMethod();
      }

      @Override
      public URI getURI() {
        return proxyUri;
      }

      @Override
      public HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(request.getHeaders());
        headers.set("Host", restTemplateConfig.getHost());
        return headers;
      }
    };
  }
}
