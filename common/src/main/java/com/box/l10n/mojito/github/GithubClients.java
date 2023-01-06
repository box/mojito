package com.box.l10n.mojito.github;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class GithubClients {

  private Map<String, GithubClient> githubOwnerToClientsCache;

  public GithubClients(GithubClientsConfiguration githubClientsConfiguration) {
    githubOwnerToClientsCache = createGithubClients(githubClientsConfiguration);
  }

  private Map<String, GithubClient> createGithubClients(
      GithubClientsConfiguration githubClientsConfiguration) {
    return githubClientsConfiguration.getGithubClients().entrySet().stream()
        .collect(
            Collectors.toMap(
                e -> e.getValue().getOwner(),
                e ->
                    new GithubClient(
                        e.getValue().getAppId(),
                        e.getValue().getKey(),
                        e.getValue().getOwner(),
                        e.getValue().getTokenTTL())));
  }

  public GithubClient getClient(String owner) {
    GithubClient githubClient = githubOwnerToClientsCache.get(owner);
    if (githubClient == null) {
      throw new GithubException(
          String.format("Github client for owner '%s' is not configured", owner));
    }
    return githubClient;
  }
}
