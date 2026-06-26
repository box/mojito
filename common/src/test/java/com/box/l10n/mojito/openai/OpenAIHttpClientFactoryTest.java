package com.box.l10n.mojito.openai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
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
    assertNotNull(OpenAIHttpClientFactory.createHttpClient(proxyConfig));
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
  public void testGetProxyPasswordAuthenticationForProxyRequest() {
    OpenAIProxyConfig proxyConfig =
        OpenAIProxyConfig.of("proxy.example.com", 3128, "proxy-user", "proxy-password");

    PasswordAuthentication authentication =
        OpenAIHttpClientFactory.getProxyPasswordAuthentication(
            proxyConfig, Authenticator.RequestorType.PROXY);

    assertNotNull(authentication);
    assertEquals("proxy-user", authentication.getUserName());
    assertEquals("proxy-password", new String(authentication.getPassword()));
  }

  @Test
  public void testGetProxyPasswordAuthenticationReturnsNullForServerRequest() {
    OpenAIProxyConfig proxyConfig =
        OpenAIProxyConfig.of("proxy.example.com", 3128, "proxy-user", "proxy-password");

    PasswordAuthentication authentication =
        OpenAIHttpClientFactory.getProxyPasswordAuthentication(
            proxyConfig, Authenticator.RequestorType.SERVER);

    assertNull(authentication);
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
    assertNotNull(OpenAIHttpClientFactory.createHttpClient(proxyConfig));
  }
}
