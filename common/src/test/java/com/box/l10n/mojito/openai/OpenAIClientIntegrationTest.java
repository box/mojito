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
import java.util.ArrayList;
import java.util.List;
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
 * l10n.ai-translate.openai-client-token} in local Mojito config (see {@link #loadLocalConfig()}).
 *
 * <p>Optional proxy (recommended to smoke an authenticating corporate webproxy before deploy):
 *
 * <ul>
 *   <li>Env: {@code OPENAI_PROXY_HOST}, {@code OPENAI_PROXY_PORT}, {@code OPENAI_PROXY_USER},
 *       {@code OPENAI_PROXY_PASSWORD}, optional {@code OPENAI_PROXY_PREFERRED_AUTH_SCHEMES}
 *   <li>Or the same {@code l10n.ai-translate.proxy-*} keys in local Mojito config
 * </ul>
 *
 * <p>By default the proxy is optional (direct OpenAI calls still count as a smoke). To force the
 * proxy path — and fail the assumption if proxy host/port are missing — set {@code
 * OPENAI_REQUIRE_PROXY=true} or {@code -Dopenai.requireProxy=true}. Use that when validating an
 * authenticating webproxy (CONNECT / 407 / preferred auth schemes).
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
  static boolean requireProxy;

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
    String preferredAuthSchemes =
        firstNonBlank(
            System.getenv("OPENAI_PROXY_PREFERRED_AUTH_SCHEMES"),
            System.getProperty("openai.proxyPreferredAuthSchemes"),
            LOCAL_CONFIG.getProperty("l10n.ai-translate.proxy-preferred-auth-schemes"));

    Integer port = null;
    if (proxyPort != null) {
      port = Integer.valueOf(proxyPort);
    }
    proxyConfig =
        OpenAIProxyConfig.of(
            proxyHost,
            port,
            proxyUser,
            proxyPassword,
            OpenAIProxyConfig.parsePreferredAuthSchemes(preferredAuthSchemes));

    model =
        firstNonBlank(
            System.getenv("OPENAI_MODEL"),
            System.getProperty("openai.model"),
            LOCAL_CONFIG.getProperty("l10n.ai-translate.model-name"),
            "gpt-4o-mini");

    requireProxy =
        Boolean.parseBoolean(
            firstNonBlank(
                System.getenv("OPENAI_REQUIRE_PROXY"),
                System.getProperty("openai.requireProxy"),
                "false"));
  }

  @Test
  public void responsesApiSimplePromptThroughConfiguredProxy() {
    assumeTrue(
        apiKey != null && !apiKey.isBlank(),
        "Set OPENAI_API_KEY (or local l10n.ai-translate.openai-client-token) to run live OpenAI smoke test");

    if (requireProxy) {
      assumeTrue(
          proxyConfig.isConfigured(),
          "OPENAI_REQUIRE_PROXY=true but proxy host/port are not configured "
              + "(set OPENAI_PROXY_* or l10n.ai-translate.proxy-host/port in local Mojito config)");
    }

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
    logger.info(
        "OpenAI smoke OK: model={}, proxyConfigured={}, output={}",
        response.model(),
        proxyConfig.isConfigured(),
        output.trim());
  }

  /**
   * Loads local Mojito config the same places developers typically keep AI settings: {@code
   * ~/.l10n/config/common/} and {@code ~/.l10n/config/webapp/} (including {@code
   * application-${user.name}.properties}). Later files override earlier keys.
   */
  static Properties loadLocalConfig() {
    Properties properties = new Properties();
    Path homeConfig = Path.of(System.getProperty("user.home"), ".l10n", "config");
    List<Path> files = new ArrayList<>();
    for (String module : new String[] {"common", "webapp"}) {
      Path dir = homeConfig.resolve(module);
      files.add(dir.resolve("application.properties"));
      files.add(dir.resolve("application-secrets.properties"));
      String user = System.getProperty("user.name");
      if (user != null && !user.isBlank()) {
        files.add(dir.resolve("application-" + user + ".properties"));
      }
    }
    for (Path file : files) {
      if (Files.isRegularFile(file)) {
        try (InputStream in = Files.newInputStream(file)) {
          properties.load(in);
          logger.info("Loaded local OpenAI IT config from {}", file);
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
