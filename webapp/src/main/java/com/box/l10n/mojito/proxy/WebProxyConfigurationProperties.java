package com.box.l10n.mojito.proxy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Shared HTTP proxy configuration used by Box SDK, AI translate, and AI review.
 *
 * <p>Per-service proxy settings (e.g. {@code l10n.boxclient.proxyHost}, {@code
 * l10n.ai-translate.proxy-host}) take precedence when host and port are both set; otherwise these
 * shared settings are used.
 */
@Component
@ConfigurationProperties(prefix = "l10n.webproxy")
public class WebProxyConfigurationProperties {

  String host;
  Integer port;
  String user;
  String password;
  boolean allowBasicTunneling;

  public boolean isConfigured() {
    return isConfigured(host, port);
  }

  public static boolean isConfigured(String host, Integer port) {
    return host != null && !host.isBlank() && port != null;
  }

  /**
   * Prefer per-service host/port/user/password when that proxy is fully configured (host + port);
   * otherwise fall back to the shared web-proxy settings.
   *
   * <p>{@code allowBasicTunneling} is OpenAI-only — read via {@link #isAllowBasicTunneling()} when
   * building the OpenAI HTTP client; Box SDK does not use it.
   */
  public ResolvedWebProxy resolve(
      String serviceHost, Integer servicePort, String serviceUser, String servicePassword) {
    if (isConfigured(serviceHost, servicePort)) {
      return new ResolvedWebProxy(serviceHost, servicePort, serviceUser, servicePassword);
    }
    if (isConfigured()) {
      return new ResolvedWebProxy(host, port, user, password);
    }
    return ResolvedWebProxy.none();
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public boolean isAllowBasicTunneling() {
    return allowBasicTunneling;
  }

  public void setAllowBasicTunneling(boolean allowBasicTunneling) {
    this.allowBasicTunneling = allowBasicTunneling;
  }
}
