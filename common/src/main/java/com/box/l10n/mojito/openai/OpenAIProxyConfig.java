package com.box.l10n.mojito.openai;

public record OpenAIProxyConfig(
    String host, Integer port, String user, String password, boolean allowBasicTunneling) {

  public static OpenAIProxyConfig of(String host, Integer port, String user, String password) {
    return of(host, port, user, password, false);
  }

  public static OpenAIProxyConfig of(
      String host, Integer port, String user, String password, boolean allowBasicTunneling) {
    return new OpenAIProxyConfig(host, port, user, password, allowBasicTunneling);
  }

  public boolean isConfigured() {
    return host != null && !host.isBlank() && port != null;
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
        + ", allowBasicTunneling="
        + allowBasicTunneling
        + "]";
  }
}
