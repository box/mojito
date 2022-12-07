package com.box.l10n.mojito.github;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n")
public class GithubClientsConfiguration {

  Map<String, GithubClientConfiguration> githubClients = new HashMap<>();

  public Map<String, GithubClientConfiguration> getGithubClients() {
    return githubClients;
  }

  public void setGithubClients(Map<String, GithubClientConfiguration> githubClientConfigurations) {
    this.githubClients = githubClientConfigurations;
  }
}
