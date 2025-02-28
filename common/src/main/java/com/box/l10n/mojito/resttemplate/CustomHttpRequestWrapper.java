package com.box.l10n.mojito.resttemplate;

import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;

public class CustomHttpRequestWrapper implements HttpRequest {
  private final HttpRequest original;
  private final HttpHeaders headers;

  public CustomHttpRequestWrapper(HttpRequest original, HttpHeaders extraHeaders) {
    this.original = original;
    this.headers = new HttpHeaders();
    this.headers.putAll(original.getHeaders());
    this.headers.putAll(extraHeaders);
  }

  @Override
  public HttpMethod getMethod() {
    return original.getMethod();
  }

  @Override
  public URI getURI() {
    return original.getURI();
  }

  @Override
  public HttpHeaders getHeaders() {
    return headers;
  }
}
