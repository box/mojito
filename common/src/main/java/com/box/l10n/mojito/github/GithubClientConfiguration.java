package com.box.l10n.mojito.github;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("l10n.github")
public class GithubClientConfiguration {

  String appId;

  String key;

  String owner;

  Long tokenTTL = 60000L;

  @ConditionalOnProperty("l10n.github.key")
  @Bean
  GithubClient getGithubClient() throws NoSuchAlgorithmException, InvalidKeySpecException {
    return new GithubClient(appId, key, owner, tokenTTL);
  }

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
}
