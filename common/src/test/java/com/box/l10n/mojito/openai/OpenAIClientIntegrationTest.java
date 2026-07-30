package com.box.l10n.mojito.openai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.box.l10n.mojito.openai.OpenAIClient.ResponsesRequest;
import com.box.l10n.mojito.openai.OpenAIClient.ResponsesResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Live smoke test against OpenAI (optionally via HTTP proxy). Skipped unless an API key is
 * available so normal unit-test runs stay offline.
 *
 * <p>Enable by setting {@code OPENAI_API_KEY}, or {@code -Dopenai.apiKey=...}, or putting {@code
 * l10n.ai-translate.openai-client-token} in {@code
 * ~/.l10n/config/common/application-secrets.properties} (or {@code application.properties}).
 *
 * <p>Optional proxy (recommended to smoke an authenticating corporate webproxy before deploy):
 *
 * <ul>
 *   <li>Env: {@code OPENAI_PROXY_HOST}, {@code OPENAI_PROXY_PORT}, {@code OPENAI_PROXY_USER},
 *       {@code OPENAI_PROXY_PASSWORD}
 *   <li>Or Mojito properties in {@code ~/.l10n/config/common/}: {@code
 *       l10n.ai-translate.proxy-host|proxy-port|proxy-user|proxy-password}
 * </ul>
 *
 * <p>OpenAI uses Apache HttpClient 5; proxy credentials are handled via a credentials provider
 * (separate from the Bearer token). If the proxy (or TLS inspection) uses a private CA, import your
 * organization's CA into the JVM truststore (or point {@code javax.net.ssl.trustStore} at a custom
 * one). Open-source users should use their own company CAs — Mojito does not ship corporate
 * certificates.
 *
 * <p>Local helper (not for commit): {@code ./run-openai-client-it.sh}
 */
public class OpenAIClientIntegrationTest {

  static final Logger logger = LoggerFactory.getLogger(OpenAIClientIntegrationTest.class);

  static final Properties LOCAL_CONFIG = loadLocalConfig();

  static String apiKey;
  static OpenAIProxyConfig proxyConfig;
  static String model;

  @BeforeAll
  static void resolveConfig() {
    apiKey =
        firstNonBlank(
            System.getenv("OPENAI_API_KEY"),
            System.getProperty("openai.apiKey"),
            LOCAL_CONFIG.getProperty("l10n.ai-translate.openai-client-token"));

    String proxyHost =
        firstNonBlank(
            System.getenv("OPENAI_PROXY_HOST"),
            System.getProperty("openai.proxyHost"),
            LOCAL_CONFIG.getProperty("l10n.ai-translate.proxy-host"));
    String proxyPort =
        firstNonBlank(
            System.getenv("OPENAI_PROXY_PORT"),
            System.getProperty("openai.proxyPort"),
            LOCAL_CONFIG.getProperty("l10n.ai-translate.proxy-port"));
    String proxyUser =
        firstNonBlank(
            System.getenv("OPENAI_PROXY_USER"),
            System.getProperty("openai.proxyUser"),
            LOCAL_CONFIG.getProperty("l10n.ai-translate.proxy-user"));
    String proxyPassword =
        firstNonBlank(
            System.getenv("OPENAI_PROXY_PASSWORD"),
            System.getProperty("openai.proxyPassword"),
            LOCAL_CONFIG.getProperty("l10n.ai-translate.proxy-password"));

    Integer port = null;
    if (proxyPort != null) {
      port = Integer.valueOf(proxyPort);
    }
    proxyConfig = OpenAIProxyConfig.of(proxyHost, port, proxyUser, proxyPassword);

    model =
        firstNonBlank(
            System.getenv("OPENAI_MODEL"),
            System.getProperty("openai.model"),
            LOCAL_CONFIG.getProperty("l10n.ai-translate.model-name"),
            "gpt-4o-mini");
  }

  @Test
  public void responsesApiSimplePromptThroughConfiguredProxy() {
    assumeTrue(
        apiKey != null && !apiKey.isBlank(),
        "Set OPENAI_API_KEY (or local l10n.ai-translate.openai-client-token) to run live OpenAI smoke test");

    OpenAIClient.Builder builder = OpenAIClient.builder().apiKey(apiKey);
    if (proxyConfig.isConfigured()) {
      logger.info("Using OpenAI proxy {}", proxyConfig);
      builder.proxyConfig(proxyConfig);
    } else {
      logger.info("No OpenAI proxy configured; calling api.openai.com directly");
    }

    OpenAIClient client = builder.build();

    ResponsesRequest request =
        ResponsesRequest.builder()
            .model(model)
            .instructions("Reply with exactly the single word: pong")
            .addUserText("ping")
            .build();

    ResponsesResponse response = client.getResponses(request, Duration.ofSeconds(60)).join();

    assertNotNull(response);
    assertNotNull(response.id());
    String output = response.outputText();
    assertNotNull(output);
    assertEquals("pong", output.trim(), "Expected exact model reply 'pong'");
    logger.info("OpenAI smoke OK: model={}, output={}", response.model(), output.trim());
  }

  static Properties loadLocalConfig() {
    Properties properties = new Properties();
    Path dir = Path.of(System.getProperty("user.home"), ".l10n", "config", "common");
    for (String name : new String[] {"application.properties", "application-secrets.properties"}) {
      Path file = dir.resolve(name);
      if (Files.isRegularFile(file)) {
        try (InputStream in = Files.newInputStream(file)) {
          properties.load(in);
          logger.debug("Loaded local OpenAI IT config from {}", file);
        } catch (IOException e) {
          logger.warn("Could not read {}", file, e);
        }
      }
    }
    return properties;
  }

  static String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return null;
  }
}
