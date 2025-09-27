package com.box.l10n.mojito.rest.resttemplate;

import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

class HeaderAuthenticationInterceptor implements ClientHttpRequestInterceptor {

  private final Map<String, String> headers;

  HeaderAuthenticationInterceptor(Map<String, String> headers) {
    this.headers = headers;
  }

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    headers.forEach(
        (name, value) -> {
          if (name != null && value != null) {
            name = name.replace(".", "-");

            request.getHeaders().set(name, value);
          }
        });
    return execution.execute(request, body);
  }
}
