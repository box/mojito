package com.box.l10n.mojito.service.oaireview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.openai.OpenAIClient;
import com.box.l10n.mojito.openai.OpenAIClientPool;
import com.box.l10n.mojito.openai.OpenAIProxyConfig;
import com.box.l10n.mojito.proxy.WebProxyConfigurationProperties;
import org.junit.Test;

public class AiReviewConfigTest {

  @Test
  public void testOpenAiBeansAreNullWithoutToken() {
    AiReviewConfigurationProperties properties = new AiReviewConfigurationProperties();
    AiReviewConfig aiReviewConfig =
        new AiReviewConfig(properties, new WebProxyConfigurationProperties());

    assertNull(aiReviewConfig.openAIClient());
    assertNull(aiReviewConfig.openAIClientPool());
  }

  @Test
  public void testOpenAiBeansUseProxyConfiguration() {
    AiReviewConfigurationProperties properties = new AiReviewConfigurationProperties();
    properties.setOpenaiClientToken("test-token");
    properties.setProxyHost("proxy.example.com");
    properties.setProxyPort(3128);
    properties.setProxyUser("proxy-user");
    properties.setProxyPassword("proxy-password");

    AiReviewConfig aiReviewConfig =
        new AiReviewConfig(properties, new WebProxyConfigurationProperties());

    OpenAIClient openAIClient = aiReviewConfig.openAIClient();
    OpenAIClientPool openAIClientPool = aiReviewConfig.openAIClientPool();

    assertNotNull(openAIClient);
    assertNotNull(openAIClientPool);
  }

  @Test
  public void testOpenAiBeansWithoutProxyConfiguration() {
    AiReviewConfigurationProperties properties = new AiReviewConfigurationProperties();
    properties.setOpenaiClientToken("test-token");

    AiReviewConfig aiReviewConfig =
        new AiReviewConfig(properties, new WebProxyConfigurationProperties());

    assertNotNull(aiReviewConfig.openAIClient());
    assertNotNull(aiReviewConfig.openAIClientPool());
  }

  @Test
  public void testGetProxyConfigPrefersPerServiceSettings() {
    AiReviewConfigurationProperties properties = new AiReviewConfigurationProperties();
    properties.setProxyHost("service-proxy.example.com");
    properties.setProxyPort(3128);
    properties.setProxyUser("proxy-user");
    properties.setProxyPassword("proxy-password");

    WebProxyConfigurationProperties webProxy = new WebProxyConfigurationProperties();
    webProxy.setHost("shared-proxy.example.com");
    webProxy.setPort(8080);
    webProxy.setAllowBasicTunneling(true);

    AiReviewConfig aiReviewConfig = new AiReviewConfig(properties, webProxy);
    OpenAIProxyConfig proxyConfig = aiReviewConfig.getProxyConfig();

    assertEquals("service-proxy.example.com", proxyConfig.host());
    assertEquals(Integer.valueOf(3128), proxyConfig.port());
    assertEquals("proxy-user", proxyConfig.user());
    assertEquals("proxy-password", proxyConfig.password());
    assertTrue(proxyConfig.allowBasicTunneling());
  }

  @Test
  public void testGetProxyConfigFallsBackToSharedWebProxy() {
    AiReviewConfigurationProperties properties = new AiReviewConfigurationProperties();

    WebProxyConfigurationProperties webProxy = new WebProxyConfigurationProperties();
    webProxy.setHost("shared-proxy.example.com");
    webProxy.setPort(8080);
    webProxy.setUser("shared-user");
    webProxy.setPassword("shared-password");
    webProxy.setAllowBasicTunneling(true);

    AiReviewConfig aiReviewConfig = new AiReviewConfig(properties, webProxy);
    OpenAIProxyConfig proxyConfig = aiReviewConfig.getProxyConfig();

    assertEquals("shared-proxy.example.com", proxyConfig.host());
    assertEquals(Integer.valueOf(8080), proxyConfig.port());
    assertEquals("shared-user", proxyConfig.user());
    assertEquals("shared-password", proxyConfig.password());
    assertTrue(proxyConfig.allowBasicTunneling());
  }

  @Test
  public void testGetProxyConfigWithNoProxy() {
    AiReviewConfig aiReviewConfig =
        new AiReviewConfig(
            new AiReviewConfigurationProperties(), new WebProxyConfigurationProperties());

    OpenAIProxyConfig proxyConfig = aiReviewConfig.getProxyConfig();

    assertFalse(proxyConfig.isConfigured());
    assertFalse(proxyConfig.allowBasicTunneling());
  }
}
