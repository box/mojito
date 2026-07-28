package com.box.l10n.mojito.openai;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executor;

public final class OpenAIHttpClientFactory {

  /**
   * Default TCP/proxy-CONNECT timeout. Without this, {@link HttpClient} waits indefinitely to
   * establish a connection, which can look like a silent hang when a proxy or network path is
   * misconfigured.
   */
  public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(30);

  private OpenAIHttpClientFactory() {}

  public static HttpClient createHttpClient(OpenAIProxyConfig proxyConfig) {
    return createHttpClient(proxyConfig, null, DEFAULT_CONNECT_TIMEOUT);
  }

  public static HttpClient createHttpClient(OpenAIProxyConfig proxyConfig, Executor executor) {
    return createHttpClient(proxyConfig, executor, DEFAULT_CONNECT_TIMEOUT);
  }

  public static HttpClient createHttpClient(
      OpenAIProxyConfig proxyConfig, Executor executor, Duration connectTimeout) {
    HttpClient.Builder builder = HttpClient.newBuilder();
    builder.connectTimeout(connectTimeout != null ? connectTimeout : DEFAULT_CONNECT_TIMEOUT);
    if (executor != null) {
      builder.executor(executor);
    }
    configureProxy(builder, proxyConfig);
    return builder.build();
  }

  /**
   * Configures proxy host/port only. Proxy Basic credentials must be sent as a preemptive {@code
   * Proxy-Authorization} header on each request (see {@link
   * OpenAIProxyConfig#proxyAuthorizationHeaderValue()}). Do not register an {@link
   * java.net.Authenticator}: on JDK &lt; 24 that strips user-set {@code Authorization} headers
   * (JDK-8326949), which drops the OpenAI Bearer token.
   */
  static void configureProxy(HttpClient.Builder builder, OpenAIProxyConfig proxyConfig) {
    if (proxyConfig != null && proxyConfig.isConfigured()) {
      builder.proxy(createProxySelector(proxyConfig));
    }
  }

  static ProxySelector createProxySelector(OpenAIProxyConfig proxyConfig) {
    return ProxySelector.of(new InetSocketAddress(proxyConfig.host(), proxyConfig.port()));
  }
}
