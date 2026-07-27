package com.box.l10n.mojito.service.oaireview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.box.l10n.mojito.openai.OpenAIClient;
import com.box.l10n.mojito.openai.OpenAIClientPool;
import com.box.l10n.mojito.openai.OpenAIProxyConfig;
import org.junit.Test;

public class AiReviewConfigTest {

  @Test
  public void testOpenAiBeansAreNullWithoutToken() {
    AiReviewConfigurationProperties properties = new AiReviewConfigurationProperties();
    AiReviewConfig aiReviewConfig = new AiReviewConfig(properties);

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

    AiReviewConfig aiReviewConfig = new AiReviewConfig(properties);

    OpenAIClient openAIClient = aiReviewConfig.openAIClient();
    OpenAIClientPool openAIClientPool = aiReviewConfig.openAIClientPool();

    assertNotNull(openAIClient);
    assertNotNull(openAIClientPool);
  }

  @Test
  public void testOpenAiBeansWithoutProxyConfiguration() {
    AiReviewConfigurationProperties properties = new AiReviewConfigurationProperties();
    properties.setOpenaiClientToken("test-token");

    AiReviewConfig aiReviewConfig = new AiReviewConfig(properties);

    assertNotNull(aiReviewConfig.openAIClient());
    assertNotNull(aiReviewConfig.openAIClientPool());
  }

  @Test
  public void testGetProxyConfigFromProperties() {
    AiReviewConfigurationProperties properties = new AiReviewConfigurationProperties();
    properties.setProxyHost("proxy.example.com");
    properties.setProxyPort(3128);
    properties.setProxyUser("proxy-user");
    properties.setProxyPassword("proxy-password");

    OpenAIProxyConfig proxyConfig = AiReviewConfig.getProxyConfig(properties);

    assertEquals("proxy.example.com", proxyConfig.host());
    assertEquals(Integer.valueOf(3128), proxyConfig.port());
    assertEquals("proxy-user", proxyConfig.user());
    assertEquals("proxy-password", proxyConfig.password());
  }
}
