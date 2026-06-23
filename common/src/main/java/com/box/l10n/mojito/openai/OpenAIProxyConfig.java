package com.box.l10n.mojito.openai;

public record OpenAIProxyConfig(String host, Integer port, String user, String password) {

  public static OpenAIProxyConfig of(String host, Integer port, String user, String password) {
    return new OpenAIProxyConfig(host, port, user, password);
  }

  public boolean isConfigured() {
    return host != null && !host.isBlank() && port != null;
  }
}
