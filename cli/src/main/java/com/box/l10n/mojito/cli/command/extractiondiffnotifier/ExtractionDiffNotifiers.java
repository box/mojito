package com.box.l10n.mojito.cli.command.extractiondiffnotifier;

import com.box.l10n.mojito.cli.command.extractiondiffnotifier.ExtractionDiffNotifiersConfigurationProperties.GithubConfigurationProperties;
import com.box.l10n.mojito.cli.command.extractiondiffnotifier.ExtractionDiffNotifiersConfigurationProperties.PhabricatorConfigurationProperties;
import com.box.l10n.mojito.cli.command.extractiondiffnotifier.ExtractionDiffNotifiersConfigurationProperties.SlackConfigurationProperties;
import com.box.l10n.mojito.github.GithubClients;
import com.box.l10n.mojito.phabricator.DifferentialRevision;
import com.box.l10n.mojito.phabricator.PhabricatorHttpClient;
import com.box.l10n.mojito.slack.SlackClient;
import com.box.l10n.mojito.slack.SlackClients;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ExtractionDiffNotifiers {

  Map<String, ExtractionDiffNotifier> mapIdToNotifiers;

  public ExtractionDiffNotifiers(
      ExtractionDiffNotifiersConfigurationProperties extractionDiffNotifiersConfigurationProperties,
      GithubClients githubClients,
      SlackClients slackClients) {
    this.mapIdToNotifiers =
        createInstancesFromConfiguration(
            extractionDiffNotifiersConfigurationProperties, githubClients, slackClients);
  }

  public ExtractionDiffNotifier getById(String id) {
    return mapIdToNotifiers.get(id);
  }

  private Map<String, ExtractionDiffNotifier> createInstancesFromConfiguration(
      ExtractionDiffNotifiersConfigurationProperties extractionDiffNotifiersConfigurationProperties,
      GithubClients githubClients,
      SlackClients slackClients) {

    Map<String, ExtractionDiffNotifier> instances = new HashMap<>();

    checkIdsStartWithPrefix(
        extractionDiffNotifiersConfigurationProperties.getGithub().keySet(), "github");
    checkIdsStartWithPrefix(
        extractionDiffNotifiersConfigurationProperties.getPhabricator().keySet(), "phabricator");
    checkIdsStartWithPrefix(
        extractionDiffNotifiersConfigurationProperties.getSlack().keySet(), "slack");

    instances.putAll(
        createGithubInstances(
            extractionDiffNotifiersConfigurationProperties.getGithub(), githubClients));

    instances.putAll(
        createPhabricatorInstances(
            extractionDiffNotifiersConfigurationProperties.getPhabricator()));

    instances.putAll(
        createSlackInstances(
            extractionDiffNotifiersConfigurationProperties.getSlack(), slackClients));

    return instances;
  }

  private Map<String, ? extends ExtractionDiffNotifier> createGithubInstances(
      Map<String, GithubConfigurationProperties> github, GithubClients githubClients) {

    return github.entrySet().stream()
        .map(
            e -> {
              String id = e.getKey();
              GithubConfigurationProperties githubConfigurationProperties = e.getValue();

              ExtractionDiffNotifierGithub extractionDiffNotifierGithub =
                  new ExtractionDiffNotifierGithub(
                      new ExtractionDiffNotifierMessageBuilder(
                          githubConfigurationProperties.getMessageTemplate()),
                      githubClients.getClient(githubConfigurationProperties.getOwner()),
                      githubConfigurationProperties.getRepository(),
                      githubConfigurationProperties.getPrNumber());

              return new SimpleEntry<String, ExtractionDiffNotifierGithub>(
                  id, extractionDiffNotifierGithub);
            })
        .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
  }

  private Map<String, ? extends ExtractionDiffNotifier> createPhabricatorInstances(
      Map<String, PhabricatorConfigurationProperties> phabricator) {
    return phabricator.entrySet().stream()
        .map(
            e -> {
              String id = e.getKey();
              PhabricatorConfigurationProperties phabricatorConfigurationProperties = e.getValue();

              PhabricatorHttpClient phabricatorHttpClient =
                  new PhabricatorHttpClient(
                      phabricatorConfigurationProperties.getUrl(),
                      phabricatorConfigurationProperties.getToken());

              DifferentialRevision differentialRevision =
                  new DifferentialRevision(phabricatorHttpClient);

              ExtractionDiffNotifierPhabricator extractionDiffNotifierPhabricator =
                  new ExtractionDiffNotifierPhabricator(
                      new ExtractionDiffNotifierMessageBuilder(
                          phabricatorConfigurationProperties.getMessageTemplate()),
                      differentialRevision,
                      phabricatorConfigurationProperties.getObjectIdentifier());

              return new SimpleEntry<String, ExtractionDiffNotifierPhabricator>(
                  id, extractionDiffNotifierPhabricator);
            })
        .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
  }

  private Map<String, ? extends ExtractionDiffNotifier> createSlackInstances(
      Map<String, SlackConfigurationProperties> slack, SlackClients slackClients) {
    return slack.entrySet().stream()
        .map(
            e -> {
              String id = e.getKey();
              SlackConfigurationProperties slackConfigurationProperties = e.getValue();

              SlackClient slackClient =
                  slackClients.getById(slackConfigurationProperties.getSlackClientId());

              ExtractionDiffNotifierSlack extractionDiffNotifierSlack =
                  new ExtractionDiffNotifierSlack(
                      slackClient,
                      slackConfigurationProperties.getUserEmailPattern(),
                      slackConfigurationProperties.isUseDirectMessage(),
                      new ExtractionDiffNotifierMessageBuilder(
                          slackConfigurationProperties.getMessageTemplate()),
                      slackConfigurationProperties.getUsername());

              return new SimpleEntry<String, ExtractionDiffNotifierSlack>(
                  id, extractionDiffNotifierSlack);
            })
        .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
  }

  private void checkIdsStartWithPrefix(Set<String> ids, String prefix) {
    String prefixWithDash = prefix + "-";
    if (!ids.stream().allMatch(key -> key.startsWith(prefixWithDash))) {
      throw new RuntimeException("name must start with prefix: " + prefixWithDash);
    }
  }
}
