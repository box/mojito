package com.box.l10n.mojito.rest.resttemplate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProxyConfig {

  @Value("${l10n.proxy.host:}")
  String host;

  @Value("${l10n.proxy.scheme:http}")
  String scheme;

  @Value("${l10n.proxy.port:19193}")
  Integer port;

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public String getScheme() {
    return scheme;
  }

  public void setScheme(String scheme) {
    this.scheme = scheme;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public boolean isValidConfiguration() {
    return this.host != null
        && this.scheme != null
        && this.port != null
        && !this.host.isEmpty()
        && !this.scheme.isEmpty()
        && this.port > 0;
  }
}
