package com.box.l10n.mojito.proxy;

/**
 * Resolved HTTP proxy settings after applying per-service overrides (if any) and shared web-proxy
 * fallback. Does not include JDK Basic-tunneling behavior — that is OpenAI-specific ({@code
 * l10n.webproxy.allowBasicTunneling}).
 */
public record ResolvedWebProxy(String host, Integer port, String user, String password) {

  public static ResolvedWebProxy none() {
    return new ResolvedWebProxy(null, null, null, null);
  }

  public boolean isConfigured() {
    return host != null && !host.isBlank() && port != null;
  }
}
