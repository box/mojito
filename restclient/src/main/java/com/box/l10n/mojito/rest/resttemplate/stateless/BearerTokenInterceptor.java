package com.box.l10n.mojito.rest.resttemplate.stateless;

import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(TokenSupplier.class)
public class BearerTokenInterceptor implements ClientHttpRequestInterceptor {

  private final TokenSupplier tokenSupplier;

  public BearerTokenInterceptor(TokenSupplier tokenSupplier) {
    this.tokenSupplier = tokenSupplier;
  }

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    String token = tokenSupplier.getAccessToken();
    if (token != null && !token.isEmpty()) {
      request.getHeaders().set("Authorization", "Bearer " + token);
    }
    return execution.execute(request, body);
  }
}
