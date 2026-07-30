package com.box.l10n.mojito.openai;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.Timeout;

/**
 * Builds Apache HttpClient 5 instances for {@link OpenAIClient}.
 *
 * <p>Proxy credentials are registered on a {@link
 * org.apache.hc.client5.http.auth.CredentialsProvider} scoped to the proxy host. That keeps proxy
 * authentication (including non-Basic schemes the client can negotiate) separate from the OpenAI
 * {@code Authorization: Bearer} header, avoiding JDK {@code java.net.http.HttpClient} issue
 * JDK-8326949.
 */
public final class OpenAIHttpClientFactory {

  /**
   * Default TCP/proxy-CONNECT timeout. Without this, connection establishment can wait
   * indefinitely, which can look like a silent hang when a proxy or network path is misconfigured.
   */
  public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(30);

  private OpenAIHttpClientFactory() {}

  public static CloseableHttpClient createHttpClient(OpenAIProxyConfig proxyConfig) {
    return createHttpClient(proxyConfig, DEFAULT_CONNECT_TIMEOUT);
  }

  public static CloseableHttpClient createHttpClient(
      OpenAIProxyConfig proxyConfig, Duration connectTimeout) {
    Duration effectiveConnectTimeout =
        connectTimeout != null ? connectTimeout : DEFAULT_CONNECT_TIMEOUT;
    Timeout connect = Timeout.of(effectiveConnectTimeout.toMillis(), TimeUnit.MILLISECONDS);

    HttpClientConnectionManager connectionManager =
        PoolingHttpClientConnectionManagerBuilder.create()
            .setDefaultConnectionConfig(
                ConnectionConfig.custom().setConnectTimeout(connect).build())
            .build();

    RequestConfig requestConfig =
        RequestConfig.custom().setConnectionRequestTimeout(connect).build();

    HttpClientBuilder builder =
        HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig);

    configureProxy(builder, proxyConfig);
    return builder.build();
  }

  static void configureProxy(HttpClientBuilder builder, OpenAIProxyConfig proxyConfig) {
    if (proxyConfig == null || !proxyConfig.isConfigured()) {
      return;
    }

    HttpHost proxy = new HttpHost(proxyConfig.host(), proxyConfig.port());
    builder.setProxy(proxy);

    if (proxyConfig.user() != null && proxyConfig.password() != null) {
      BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
      credentialsProvider.setCredentials(
          new AuthScope(proxy),
          new UsernamePasswordCredentials(
              proxyConfig.user(), proxyConfig.password().toCharArray()));
      builder.setDefaultCredentialsProvider(credentialsProvider);
    }
  }
}
