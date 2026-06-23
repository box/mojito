package com.box.l10n.mojito.service.oaitranslate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.box.l10n.mojito.openai.OpenAIClient;
import com.box.l10n.mojito.openai.OpenAIClientPool;
import com.box.l10n.mojito.openai.OpenAIProxyConfig;
import org.junit.Test;

public class AiTranslateConfigTest {

  @Test
  public void testOpenAiBeansAreNullWithoutToken() {
    AiTranslateConfigurationProperties properties = new AiTranslateConfigurationProperties();
    AiTranslateConfig aiTranslateConfig = new AiTranslateConfig(properties);

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

    AiTranslateConfig aiTranslateConfig = new AiTranslateConfig(properties);

    OpenAIClient openAIClient = aiTranslateConfig.openAIClient();
    OpenAIClientPool openAIClientPool = aiTranslateConfig.openAIClientPool();

    assertNotNull(openAIClient);
    assertNotNull(openAIClientPool);
  }

  @Test
  public void testOpenAiBeansWithoutProxyConfiguration() {
    AiTranslateConfigurationProperties properties = new AiTranslateConfigurationProperties();
    properties.setOpenaiClientToken("test-token");

    AiTranslateConfig aiTranslateConfig = new AiTranslateConfig(properties);

    assertNotNull(aiTranslateConfig.openAIClient());
    assertNotNull(aiTranslateConfig.openAIClientPool());
  }

  @Test
  public void testGetProxyConfigFromProperties() {
    AiTranslateConfigurationProperties properties = new AiTranslateConfigurationProperties();
    properties.setProxyHost("proxy.example.com");
    properties.setProxyPort(3128);
    properties.setProxyUser("proxy-user");
    properties.setProxyPassword("proxy-password");

    OpenAIProxyConfig proxyConfig = AiTranslateConfig.getProxyConfig(properties);

    assertEquals("proxy.example.com", proxyConfig.host());
    assertEquals(Integer.valueOf(3128), proxyConfig.port());
    assertEquals("proxy-user", proxyConfig.user());
    assertEquals("proxy-password", proxyConfig.password());
  }
}
