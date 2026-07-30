package com.box.l10n.mojito.openai;

import java.util.Arrays;
import java.util.List;

public record OpenAIProxyConfig(
    String host, Integer port, String user, String password, List<String> preferredAuthSchemes) {

  public OpenAIProxyConfig {
    preferredAuthSchemes =
        preferredAuthSchemes == null || preferredAuthSchemes.isEmpty()
            ? List.of()
            : List.copyOf(preferredAuthSchemes);
  }

  public static OpenAIProxyConfig of(String host, Integer port, String user, String password) {
    return of(host, port, user, password, null);
  }

  public static OpenAIProxyConfig of(
      String host, Integer port, String user, String password, List<String> preferredAuthSchemes) {
    return new OpenAIProxyConfig(host, port, user, password, preferredAuthSchemes);
  }

  /**
   * Parses a comma-separated list of HttpClient 5 auth scheme names (for example {@code
   * Basic,Digest}). Blank or null yields an empty list (library defaults).
   */
  public static List<String> parsePreferredAuthSchemes(String csv) {
    if (csv == null || csv.isBlank()) {
      return List.of();
    }
    return Arrays.stream(csv.split(","))
        .map(String::trim)
        .filter(scheme -> !scheme.isEmpty())
        .toList();
  }

  public boolean isConfigured() {
    return host != null && !host.isBlank() && port != null;
  }

  public boolean hasPreferredAuthSchemes() {
    return !preferredAuthSchemes.isEmpty();
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
        + ", preferredAuthSchemes="
        + preferredAuthSchemes
        + "]";
  }
}
