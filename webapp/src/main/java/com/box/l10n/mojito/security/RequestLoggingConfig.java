package com.box.l10n.mojito.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "l10n.logging.requests")
public class RequestLoggingConfig {

  private boolean includesQueryString = true;
  private boolean includesHeader = false;
  private boolean includesPayload = true;
  private int maxPayloadLength = 10000;
  private String beforeMessagePrefix = "Request Data: [ ";
  private String afterMessagePrefix = " ]";
  private boolean enabled = false;

  public boolean includesQueryString() {
    return includesQueryString;
  }

  public void setIncludesQueryString(boolean includesQueryString) {
    this.includesQueryString = includesQueryString;
  }

  public boolean includesHeader() {
    return includesHeader;
  }

  public void setIncludesHeader(boolean includesHeader) {
    this.includesHeader = includesHeader;
  }

  public boolean includesPayload() {
    return includesPayload;
  }

  public void setIncludesPayload(boolean includesPayload) {
    this.includesPayload = includesPayload;
  }

  public int getMaxPayloadLength() {
    return maxPayloadLength;
  }

  public void setMaxPayloadLength(int maxPayloadLength) {
    this.maxPayloadLength = maxPayloadLength;
  }

  public String getBeforeMessagePrefix() {
    return beforeMessagePrefix;
  }

  public void setBeforeMessagePrefix(String beforeMessagePrefix) {
    this.beforeMessagePrefix = beforeMessagePrefix;
  }

  public String getAfterMessagePrefix() {
    return afterMessagePrefix;
  }

  public void setAfterMessagePrefix(String afterMessagePrefix) {
    this.afterMessagePrefix = afterMessagePrefix;
  }

  public boolean isLoggingEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
