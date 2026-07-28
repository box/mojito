package com.box.l10n.mojito.openai;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public record OpenAIProxyConfig(String host, Integer port, String user, String password) {

  public static OpenAIProxyConfig of(String host, Integer port, String user, String password) {
    return new OpenAIProxyConfig(host, port, user, password);
  }

  public boolean isConfigured() {
    return host != null && !host.isBlank() && port != null;
  }

  /**
   * Whether proxy Basic credentials are available for a preemptive {@code Proxy-Authorization}
   * header.
   */
  public boolean hasCredentials() {
    return user != null && !user.isBlank() && password != null;
  }

  /**
   * Value for the {@code Proxy-Authorization} request header (including the {@code Basic } prefix).
   *
   * <p>Used instead of {@link java.net.http.HttpClient.Builder#authenticator} because on JDK &lt;
   * 24, registering an Authenticator causes the client to strip user-set {@code Authorization}
   * headers (JDK-8326949), which drops the OpenAI Bearer token.
   */
  public String proxyAuthorizationHeaderValue() {
    if (!hasCredentials()) {
      throw new IllegalStateException("Proxy credentials are not configured");
    }
    String token =
        Base64.getEncoder()
            .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
    return "Basic " + token;
  }

  @Override
  public String toString() {
    return "OpenAIProxyConfig[host="
        + host
        + ", port="
        + port
        + ", user="
        + (user == null ? "null" : "***")
        + ", password="
        + (password == null ? "null" : "***")
        + "]";
  }
}
