package com.box.l10n.mojito.github;

public class GithubClientConfiguration {

  String appId;

  String key;

  String owner;

  Long tokenTTL = 60000L;

  String endpoint = "https://api.github.com";

  int maxRetries = 3;

  int retryMinBackoffSecs = 5;

  int retryMaxBackoffSecs = 60;

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public Long getTokenTTL() {
    return tokenTTL;
  }

  public void setTokenTTL(Long tokenTTL) {
    this.tokenTTL = tokenTTL;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public int getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(int maxRetries) {
    this.maxRetries = maxRetries;
  }

  public int getRetryMinBackoffSecs() {
    return retryMinBackoffSecs;
  }

  public void setRetryMinBackoffSecs(int retryMinBackoffSecs) {
    this.retryMinBackoffSecs = retryMinBackoffSecs;
  }

  public int getRetryMaxBackoffSecs() {
    return retryMaxBackoffSecs;
  }

  public void setRetryMaxBackoffSecs(int retryMaxBackoffSecs) {
    this.retryMaxBackoffSecs = retryMaxBackoffSecs;
  }
}
