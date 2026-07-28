package com.box.l10n.mojito.openai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class OpenAIClientProxyConfigTest {

  @Test
  public void testOpenAIClientBuilderWithProxyConfig() {
    OpenAIProxyConfig proxyConfig =
        OpenAIProxyConfig.of("proxy.example.com", 3128, "proxy-user", "proxy-password");

    OpenAIClient openAIClient =
        OpenAIClient.builder().apiKey("test-api-key").proxyConfig(proxyConfig).build();

    assertNotNull(openAIClient.httpClient);
    assertTrue(openAIClient.httpClient.authenticator().isEmpty());
  }

  @Test
  public void testOpenAIClientBuilderWithoutProxyConfig() {
    OpenAIClient openAIClient = OpenAIClient.builder().apiKey("test-api-key").build();

    assertNotNull(openAIClient.httpClient);
  }

  @Test
  public void testNewRequestBuilderIncludesBearerAndProxyAuthorization() {
    OpenAIProxyConfig proxyConfig =
        OpenAIProxyConfig.of("proxy.example.com", 3128, "proxy-user", "proxy-password");
    OpenAIClient openAIClient =
        OpenAIClient.builder().apiKey("test-api-key").proxyConfig(proxyConfig).build();

    HttpRequest request =
        openAIClient
            .newRequestBuilder()
            .uri(URI.create("https://api.openai.com/v1/responses"))
            .GET()
            .build();

    assertEquals(Optional.of("Bearer test-api-key"), request.headers().firstValue("Authorization"));
    assertEquals(
        Optional.of(
            "Basic "
                + Base64.getEncoder()
                    .encodeToString("proxy-user:proxy-password".getBytes(StandardCharsets.UTF_8))),
        request.headers().firstValue("Proxy-Authorization"));
  }

  @Test
  public void testNewRequestBuilderOmitsProxyAuthorizationWithoutCredentials() {
    OpenAIClient openAIClient =
        OpenAIClient.builder()
            .apiKey("test-api-key")
            .proxyConfig(OpenAIProxyConfig.of("proxy.example.com", 3128, null, null))
            .build();

    HttpRequest request =
        openAIClient
            .newRequestBuilder()
            .uri(URI.create("https://api.openai.com/v1/responses"))
            .GET()
            .build();

    assertEquals(Optional.of("Bearer test-api-key"), request.headers().firstValue("Authorization"));
    assertFalse(request.headers().firstValue("Proxy-Authorization").isPresent());
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

    OpenAIClient openAIClient = openAIClientPool.openAIClientWithSemaphores[0].openAIClient();
    assertNotNull(openAIClient.httpClient);
    assertTrue(openAIClient.httpClient.authenticator().isEmpty());

    HttpRequest request =
        openAIClient
            .newRequestBuilder()
            .uri(URI.create("https://api.openai.com/v1/responses"))
            .GET()
            .build();
    assertTrue(request.headers().firstValue("Proxy-Authorization").isPresent());
  }
}
