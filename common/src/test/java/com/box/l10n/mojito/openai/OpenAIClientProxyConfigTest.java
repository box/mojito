package com.box.l10n.mojito.openai;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class OpenAIClientProxyConfigTest {

  @Test
  public void testOpenAIClientBuilderWithProxyConfig() {
    OpenAIProxyConfig proxyConfig =
        OpenAIProxyConfig.of("proxy.example.com", 3128, "proxy-user", "proxy-password");

    OpenAIClient openAIClient =
        OpenAIClient.builder().apiKey("test-api-key").proxyConfig(proxyConfig).build();

    assertNotNull(openAIClient.httpClient);
  }

  @Test
  public void testOpenAIClientBuilderWithoutProxyConfig() {
    OpenAIClient openAIClient = OpenAIClient.builder().apiKey("test-api-key").build();

    assertNotNull(openAIClient.httpClient);
  }

  @Test
  public void testOpenAIClientPoolWithoutProxyConfig() {
    OpenAIClientPool openAIClientPool = new OpenAIClientPool(1, 1, 1, "test-api-key");

    assertNotNull(openAIClientPool);
    assertNotNull(openAIClientPool.openAIClientWithSemaphores[0].openAIClient().httpClient);
  }

  @Test
  public void testOpenAIClientPoolWithProxyConfig() {
    OpenAIProxyConfig proxyConfig =
        OpenAIProxyConfig.of("proxy.example.com", 3128, "proxy-user", "proxy-password");

    OpenAIClientPool openAIClientPool = new OpenAIClientPool(1, 1, 1, "test-api-key", proxyConfig);

    assertNotNull(openAIClientPool);
    assertNotNull(openAIClientPool.openAIClientWithSemaphores[0].openAIClient().httpClient);
  }
}
