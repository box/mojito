package com.box.l10n.mojito.service.oaitranslate;

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

public class AiTranslateConfigTest {

  @Test
  public void testOpenAiBeansAreNullWithoutToken() {
    AiTranslateConfigurationProperties properties = new AiTranslateConfigurationProperties();
    AiTranslateConfig aiTranslateConfig =
        new AiTranslateConfig(properties, new WebProxyConfigurationProperties());

    assertNull(aiTranslateConfig.openAIClient());
    assertNull(aiTranslateConfig.openAIClientPool());
  }

  @Test
  public void testOpenAiBeansUseProxyConfiguration() {
    AiTranslateConfigurationProperties properties = new AiTranslateConfigurationProperties();
    properties.setOpenaiClientToken("test-token");
    properties.setProxyHost("proxy.example.com");
    properties.setProxyPort(3128);
    properties.setProxyUser("proxy-user");
    properties.setProxyPassword("proxy-password");

    AiTranslateConfig aiTranslateConfig =
        new AiTranslateConfig(properties, new WebProxyConfigurationProperties());

    OpenAIClient openAIClient = aiTranslateConfig.openAIClient();
    OpenAIClientPool openAIClientPool = aiTranslateConfig.openAIClientPool();

    assertNotNull(openAIClient);
    assertNotNull(openAIClientPool);
  }

  @Test
  public void testOpenAiBeansWithoutProxyConfiguration() {
    AiTranslateConfigurationProperties properties = new AiTranslateConfigurationProperties();
    properties.setOpenaiClientToken("test-token");

    AiTranslateConfig aiTranslateConfig =
        new AiTranslateConfig(properties, new WebProxyConfigurationProperties());

    assertNotNull(aiTranslateConfig.openAIClient());
    assertNotNull(aiTranslateConfig.openAIClientPool());
  }

  @Test
  public void testGetProxyConfigPrefersPerServiceSettings() {
    AiTranslateConfigurationProperties properties = new AiTranslateConfigurationProperties();
    properties.setProxyHost("service-proxy.example.com");
    properties.setProxyPort(3128);
    properties.setProxyUser("proxy-user");
    properties.setProxyPassword("proxy-password");

    WebProxyConfigurationProperties webProxy = new WebProxyConfigurationProperties();
    webProxy.setHost("shared-proxy.example.com");
    webProxy.setPort(8080);
    webProxy.setUser("shared-user");
    webProxy.setPassword("shared-password");
    webProxy.setAllowBasicTunneling(true);

    AiTranslateConfig aiTranslateConfig = new AiTranslateConfig(properties, webProxy);
    OpenAIProxyConfig proxyConfig = aiTranslateConfig.getProxyConfig();

    assertEquals("service-proxy.example.com", proxyConfig.host());
    assertEquals(Integer.valueOf(3128), proxyConfig.port());
    assertEquals("proxy-user", proxyConfig.user());
    assertEquals("proxy-password", proxyConfig.password());
    assertTrue(proxyConfig.allowBasicTunneling());
  }

  @Test
  public void testGetProxyConfigFallsBackToSharedWebProxy() {
    AiTranslateConfigurationProperties properties = new AiTranslateConfigurationProperties();

    WebProxyConfigurationProperties webProxy = new WebProxyConfigurationProperties();
    webProxy.setHost("shared-proxy.example.com");
    webProxy.setPort(8080);
    webProxy.setUser("shared-user");
    webProxy.setPassword("shared-password");
    webProxy.setAllowBasicTunneling(true);

    AiTranslateConfig aiTranslateConfig = new AiTranslateConfig(properties, webProxy);
    OpenAIProxyConfig proxyConfig = aiTranslateConfig.getProxyConfig();

    assertEquals("shared-proxy.example.com", proxyConfig.host());
    assertEquals(Integer.valueOf(8080), proxyConfig.port());
    assertEquals("shared-user", proxyConfig.user());
    assertEquals("shared-password", proxyConfig.password());
    assertTrue(proxyConfig.allowBasicTunneling());
  }

  @Test
  public void testGetProxyConfigWithNoProxy() {
    AiTranslateConfig aiTranslateConfig =
        new AiTranslateConfig(
            new AiTranslateConfigurationProperties(), new WebProxyConfigurationProperties());

    OpenAIProxyConfig proxyConfig = aiTranslateConfig.getProxyConfig();

    assertFalse(proxyConfig.isConfigured());
    assertFalse(proxyConfig.allowBasicTunneling());
  }
}
