package com.box.l10n.mojito.openai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
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
  public void testCreateHttpClientWithoutProxy() throws Exception {
    assertNotNull(OpenAIHttpClientFactory.createHttpClient(null));
    assertNotNull(
        OpenAIHttpClientFactory.createHttpClient(OpenAIProxyConfig.of(null, null, null, null)));
  }

  @Test
  public void testCreateHttpClientWithProxy() {
    OpenAIProxyConfig proxyConfig =
        OpenAIProxyConfig.of("proxy.example.com", 3128, "proxy-user", "proxy-password");
    assertNotNull(OpenAIHttpClientFactory.createHttpClient(proxyConfig));
  }

  @Test
  public void testCreateHttpClientWithProxyHostAndPortOnly() {
    OpenAIProxyConfig proxyConfig = OpenAIProxyConfig.of("proxy.example.com", 3128, null, null);
    assertNotNull(OpenAIHttpClientFactory.createHttpClient(proxyConfig));
  }

  @Test
  public void testConfigureProxySkipsIncompleteConfig() {
    assertNotNull(
        OpenAIHttpClientFactory.createHttpClient(
            OpenAIProxyConfig.of("proxy.example.com", null, "proxy-user", "proxy-password")));
  }

  /**
   * Guards against infinite hangs when a proxy/host is unreachable — failures should surface as a
   * connect timeout instead of waiting forever.
   */
  @Test
  public void testConnectTimeoutFailsQuicklyAgainstUnreachableHost() throws Exception {
    Duration connectTimeout = Duration.ofMillis(500);
    try (CloseableHttpClient httpClient =
        OpenAIHttpClientFactory.createHttpClient(null, connectTimeout)) {
      // 192.0.2.1 is from TEST-NET-1 (192.0.2.0/24, RFC 5737) — reserved for documentation
      // examples, not for local/LAN use (unlike 192.168.x.x). It is intentionally non-routable
      // so connection establishment must time out rather than reach a real host.
      // https://www.rfc-editor.org/rfc/rfc5737
      HttpGet request = new HttpGet("http://192.0.2.1:81/");

      long startedAt = System.nanoTime();
      IOException thrown =
          assertThrows(IOException.class, () -> httpClient.execute(request, response -> null));
      long elapsedMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();

      assertInstanceOf(ConnectTimeoutException.class, thrown);
      assertTrue(
          elapsedMs < 3_000,
          "connect timeout should fail quickly, took " + elapsedMs + "ms (limit 3000ms)");
    }
  }
}
