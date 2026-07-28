package com.box.l10n.mojito.openai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

public class OpenAIHttpClientFactoryTest {

  @Test
  public void testOpenAIProxyConfigIsConfigured() {
    assertFalse(OpenAIProxyConfig.of(null, 3128, "user", "pass").isConfigured());
    assertFalse(OpenAIProxyConfig.of("   ", 3128, "user", "pass").isConfigured());
    assertFalse(OpenAIProxyConfig.of("proxy.example.com", null, "user", "pass").isConfigured());
    assertTrue(OpenAIProxyConfig.of("proxy.example.com", 3128, "user", "pass").isConfigured());
  }

  @Test
  public void testOpenAIProxyConfigHasCredentials() {
    assertTrue(OpenAIProxyConfig.of("proxy.example.com", 3128, "user", "pass").hasCredentials());
    assertTrue(OpenAIProxyConfig.of("proxy.example.com", 3128, "user", "").hasCredentials());
    assertFalse(OpenAIProxyConfig.of("proxy.example.com", 3128, null, "pass").hasCredentials());
    assertFalse(OpenAIProxyConfig.of("proxy.example.com", 3128, "  ", "pass").hasCredentials());
    assertFalse(OpenAIProxyConfig.of("proxy.example.com", 3128, "user", null).hasCredentials());
  }

  @Test
  public void testOpenAIProxyConfigProxyAuthorizationHeaderValue() {
    OpenAIProxyConfig proxyConfig =
        OpenAIProxyConfig.of("proxy.example.com", 3128, "proxy-user", "proxy-password");

    String expected =
        "Basic "
            + Base64.getEncoder()
                .encodeToString("proxy-user:proxy-password".getBytes(StandardCharsets.UTF_8));

    assertEquals(expected, proxyConfig.proxyAuthorizationHeaderValue());
    assertThrows(
        IllegalStateException.class,
        () ->
            OpenAIProxyConfig.of("proxy.example.com", 3128, null, null)
                .proxyAuthorizationHeaderValue());
  }

  @Test
  public void testOpenAIProxyConfigToStringRedactsCredentials() {
    OpenAIProxyConfig proxyConfig =
        OpenAIProxyConfig.of("proxy.example.com", 3128, "proxy-user", "proxy-password");

    assertEquals(
        "OpenAIProxyConfig[host=proxy.example.com, port=3128, user=***, password=***]",
        proxyConfig.toString());
    assertEquals(
        "OpenAIProxyConfig[host=proxy.example.com, port=3128, user=null, password=null]",
        OpenAIProxyConfig.of("proxy.example.com", 3128, null, null).toString());
  }

  @Test
  public void testCreateHttpClientWithoutProxy() {
    assertNotNull(OpenAIHttpClientFactory.createHttpClient(null));
    assertNotNull(
        OpenAIHttpClientFactory.createHttpClient(OpenAIProxyConfig.of(null, null, null, null)));
  }

  @Test
  public void testCreateHttpClientWithProxy() {
    OpenAIProxyConfig proxyConfig =
        OpenAIProxyConfig.of("proxy.example.com", 3128, "proxy-user", "proxy-password");
    HttpClient httpClient = OpenAIHttpClientFactory.createHttpClient(proxyConfig);

    assertNotNull(httpClient);
    // Must not register Authenticator — that strips Bearer auth on JDK < 24 (JDK-8326949).
    assertTrue(httpClient.authenticator().isEmpty());
  }

  @Test
  public void testCreateHttpClientSetsDefaultConnectTimeout() {
    HttpClient httpClient = OpenAIHttpClientFactory.createHttpClient(null);

    assertEquals(
        OpenAIHttpClientFactory.DEFAULT_CONNECT_TIMEOUT, httpClient.connectTimeout().orElseThrow());
  }

  @Test
  public void testCreateHttpClientUsesCustomConnectTimeout() {
    Duration connectTimeout = Duration.ofSeconds(5);
    HttpClient httpClient = OpenAIHttpClientFactory.createHttpClient(null, null, connectTimeout);

    assertEquals(connectTimeout, httpClient.connectTimeout().orElseThrow());
  }

  /**
   * Guards against infinite hangs when a proxy/host is unreachable — failures should surface as a
   * connect timeout instead of waiting forever.
   */
  @Test
  public void testConnectTimeoutFailsQuicklyAgainstUnreachableHost() throws Exception {
    Duration connectTimeout = Duration.ofMillis(500);
    HttpClient httpClient = OpenAIHttpClientFactory.createHttpClient(null, null, connectTimeout);

    // TEST-NET-1 (RFC 5737) — should not be routable; connection establishment must time out.
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://192.0.2.1:81/"))
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build();

    long startedAt = System.nanoTime();
    IOException thrown =
        assertThrows(
            IOException.class,
            () -> httpClient.send(request, HttpResponse.BodyHandlers.discarding()));
    long elapsedMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();

    assertInstanceOf(HttpConnectTimeoutException.class, thrown);
    assertTrue(
        elapsedMs < 3_000,
        "connect timeout should fail quickly, took " + elapsedMs + "ms (limit 3000ms)");
  }

  @Test
  public void testCreateProxySelector() {
    OpenAIProxyConfig proxyConfig =
        OpenAIProxyConfig.of("proxy.example.com", 3128, "proxy-user", "proxy-password");

    ProxySelector proxySelector = OpenAIHttpClientFactory.createProxySelector(proxyConfig);
    List<Proxy> proxies = proxySelector.select(URI.create("https://api.openai.com/v1/models"));

    assertEquals(1, proxies.size());
    assertEquals(Proxy.Type.HTTP, proxies.get(0).type());
    InetSocketAddress address = (InetSocketAddress) proxies.get(0).address();
    assertEquals("proxy.example.com", address.getHostString());
    assertEquals(3128, address.getPort());
  }

  @Test
  public void testConfigureProxySkipsIncompleteConfig() {
    assertNotNull(
        OpenAIHttpClientFactory.createHttpClient(
            OpenAIProxyConfig.of("proxy.example.com", null, "proxy-user", "proxy-password")));
  }

  @Test
  public void testCreateHttpClientWithProxyHostAndPortOnly() {
    OpenAIProxyConfig proxyConfig = OpenAIProxyConfig.of("proxy.example.com", 3128, null, null);

    ProxySelector proxySelector = OpenAIHttpClientFactory.createProxySelector(proxyConfig);
    List<Proxy> proxies = proxySelector.select(URI.create("https://api.openai.com/v1/models"));

    assertEquals(1, proxies.size());
    HttpClient httpClient = OpenAIHttpClientFactory.createHttpClient(proxyConfig);
    assertNotNull(httpClient);
    assertTrue(httpClient.authenticator().isEmpty());
  }
}
