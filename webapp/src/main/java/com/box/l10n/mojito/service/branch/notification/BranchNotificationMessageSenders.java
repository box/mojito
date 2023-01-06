package com.box.l10n.mojito.service.branch.notification;

import com.box.l10n.mojito.github.GithubClient;
import com.box.l10n.mojito.github.GithubClients;
import com.box.l10n.mojito.phabricator.DifferentialRevision;
import com.box.l10n.mojito.phabricator.PhabricatorHttpClient;
import com.box.l10n.mojito.service.branch.BranchUrlBuilder;
import com.box.l10n.mojito.service.branch.notification.BranchNotificationMessageSendersConfigurationProperties.GithubConfigurationProperties;
import com.box.l10n.mojito.service.branch.notification.BranchNotificationMessageSendersConfigurationProperties.NoopConfigurationProperties;
import com.box.l10n.mojito.service.branch.notification.BranchNotificationMessageSendersConfigurationProperties.PhabricatorConfigurationProperties;
import com.box.l10n.mojito.service.branch.notification.BranchNotificationMessageSendersConfigurationProperties.SlackConfigurationProperties;
import com.box.l10n.mojito.service.branch.notification.BranchNotificationMessageSendersConfigurationProperties.SlackConfigurationProperties.MessageBuilderConfigurationProperties;
import com.box.l10n.mojito.service.branch.notification.github.BranchNotificationMessageSenderGithub;
import com.box.l10n.mojito.service.branch.notification.noop.BranchNotificationMessageSenderNoop;
import com.box.l10n.mojito.service.branch.notification.phabricator.BranchNotificationMessageBuilderPhabricator;
import com.box.l10n.mojito.service.branch.notification.phabricator.BranchNotificationMessageSenderPhabricator;
import com.box.l10n.mojito.service.branch.notification.slack.BranchNotificationMessageBuilderSlack;
import com.box.l10n.mojito.service.branch.notification.slack.BranchNotificationMessageSenderSlack;
import com.box.l10n.mojito.slack.SlackClient;
import com.box.l10n.mojito.slack.SlackClients;
import com.box.l10n.mojito.thirdpartynotification.slack.SlackChannels;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class BranchNotificationMessageSenders {

  final Map<String, BranchNotificationMessageSender> mapIdToBranchNotificationMessageSender;

  public BranchNotificationMessageSenders(
      BranchNotificationMessageSendersConfigurationProperties
          branchNotificationMessageSendersConfigurationProperties,
      GithubClients githubClients,
      SlackClients slackClients,
      BranchUrlBuilder branchUrlBuilder) {
    this.mapIdToBranchNotificationMessageSender =
        createInstancesFromConfiguration(
            branchNotificationMessageSendersConfigurationProperties,
            githubClients,
            slackClients,
            branchUrlBuilder);
  }

  public BranchNotificationMessageSender getById(String id) {
    return mapIdToBranchNotificationMessageSender.get(id);
  }

  private Map<String, BranchNotificationMessageSender> createInstancesFromConfiguration(
      BranchNotificationMessageSendersConfigurationProperties
          branchNotificationMessageSendersConfigurationProperties,
      GithubClients githubClients,
      SlackClients slackClients,
      BranchUrlBuilder branchUrlBuilder) {

    checkIdsStartWithPrefix(
        branchNotificationMessageSendersConfigurationProperties.getNoop().keySet(), "noop");

    checkIdsStartWithPrefix(
        branchNotificationMessageSendersConfigurationProperties.getSlack().keySet(), "slack");

    checkIdsStartWithPrefix(
        branchNotificationMessageSendersConfigurationProperties.getPhabricator().keySet(),
        "phabricator");

    checkIdsStartWithPrefix(
        branchNotificationMessageSendersConfigurationProperties.getGithub().keySet(), "github");

    Map<String, BranchNotificationMessageSender> mapIdToBranchNotificationMessageSender =
        new HashMap<>();

    mapIdToBranchNotificationMessageSender.putAll(
        createNoopInstances(branchNotificationMessageSendersConfigurationProperties));

    mapIdToBranchNotificationMessageSender.putAll(
        createSlackInstances(
            branchNotificationMessageSendersConfigurationProperties,
            slackClients,
            branchUrlBuilder));

    mapIdToBranchNotificationMessageSender.putAll(
        createPhabricatorInstances(
            branchNotificationMessageSendersConfigurationProperties, branchUrlBuilder));

    mapIdToBranchNotificationMessageSender.putAll(
        createGithubInstances(
            branchNotificationMessageSendersConfigurationProperties, githubClients));

    return mapIdToBranchNotificationMessageSender;
  }

  private void checkIdsStartWithPrefix(Set<String> ids, String prefix) {
    String prefixWithDash = prefix + "-";
    if (!ids.stream().allMatch(key -> key.startsWith(prefixWithDash))) {
      throw new RuntimeException("name must start with prefix: " + prefixWithDash);
    }
  }

  private Map<String, BranchNotificationMessageSenderNoop> createNoopInstances(
      BranchNotificationMessageSendersConfigurationProperties
          branchNotificationMessageSendersConfigurationProperties) {
    Map<String, BranchNotificationMessageSenderNoop> noop =
        branchNotificationMessageSendersConfigurationProperties.getNoop().entrySet().stream()
            .map(
                e -> {
                  String id = e.getKey();
                  NoopConfigurationProperties noopConfigurationProperties = e.getValue();
                  if (!noopConfigurationProperties.isEnabled()) {
                    throw new RuntimeException("only enabled=true is accepted value");
                  }
                  BranchNotificationMessageSenderNoop branchNotificationMessageSenderNoop =
                      new BranchNotificationMessageSenderNoop(id);
                  return new SimpleEntry<String, BranchNotificationMessageSenderNoop>(
                      id, branchNotificationMessageSenderNoop);
                })
            .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    return noop;
  }

  private Map<String, BranchNotificationMessageSenderSlack> createSlackInstances(
      BranchNotificationMessageSendersConfigurationProperties
          branchNotificationMessageSendersConfigurationProperties,
      SlackClients slackClients,
      BranchUrlBuilder branchUrlBuilder) {
    Map<String, BranchNotificationMessageSenderSlack> slack =
        branchNotificationMessageSendersConfigurationProperties.getSlack().entrySet().stream()
            .map(
                e -> {
                  String id = e.getKey();
                  SlackConfigurationProperties slackConfigurationProperties = e.getValue();

                  SlackClient slackClient =
                      slackClients.getById(slackConfigurationProperties.getSlackClientId());

                  SlackChannels slackChannels = new SlackChannels(slackClient);

                  MessageBuilderConfigurationProperties messages =
                      slackConfigurationProperties.getMessages();
                  BranchNotificationMessageBuilderSlack branchNotificationMessageBuilderSlack =
                      new BranchNotificationMessageBuilderSlack(
                          branchUrlBuilder,
                          messages.getNewStrings(),
                          messages.getUpdatedStrings(),
                          messages.getTranslationsReady(),
                          messages.getScreenshotsMissing(),
                          messages.getNoMoreStrings());

                  BranchNotificationMessageSenderSlack branchNotificationMessageSenderSlack =
                      new BranchNotificationMessageSenderSlack(
                          id,
                          slackClient,
                          slackChannels,
                          branchNotificationMessageBuilderSlack,
                          slackConfigurationProperties.getUserEmailPattern(),
                          slackConfigurationProperties.isUseDirectMessage());

                  return new SimpleEntry<String, BranchNotificationMessageSenderSlack>(
                      id, branchNotificationMessageSenderSlack);
                })
            .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    return slack;
  }

  private Map<String, BranchNotificationMessageSenderPhabricator> createPhabricatorInstances(
      BranchNotificationMessageSendersConfigurationProperties
          branchNotificationMessageSendersConfigurationProperties,
      BranchUrlBuilder branchUrlBuilder) {
    Map<String, BranchNotificationMessageSenderPhabricator> phabricator =
        branchNotificationMessageSendersConfigurationProperties.getPhabricator().entrySet().stream()
            .map(
                e -> {
                  String id = e.getKey();
                  PhabricatorConfigurationProperties phabricatorConfigurationProperties =
                      e.getValue();

                  PhabricatorHttpClient phabricatorHttpClient =
                      new PhabricatorHttpClient(
                          phabricatorConfigurationProperties.getUrl(),
                          phabricatorConfigurationProperties.getToken());

                  DifferentialRevision differentialRevision =
                      new DifferentialRevision(phabricatorHttpClient);

                  PhabricatorConfigurationProperties.MessageBuilderConfigurationProperties
                      messages = phabricatorConfigurationProperties.getMessages();

                  BranchNotificationMessageBuilderPhabricator
                      branchNotificationMessageBuilderPhabricator =
                          new BranchNotificationMessageBuilderPhabricator(
                              branchUrlBuilder,
                              messages.getNewNotificationMsgFormat(),
                              messages.getUpdatedNotificationMsgFormat(),
                              messages.getNewStrings(),
                              messages.getUpdatedStrings(),
                              messages.getNoMoreStrings(),
                              messages.getTranslationsReady(),
                              messages.getScreenshotsMissing());

                  BranchNotificationMessageSenderPhabricator
                      branchNotificationMessageSenderPhabricator =
                          new BranchNotificationMessageSenderPhabricator(
                              id,
                              differentialRevision,
                              phabricatorConfigurationProperties.getReviewer(),
                              phabricatorConfigurationProperties.isBlockingReview(),
                              branchNotificationMessageBuilderPhabricator);
                  return new SimpleEntry<String, BranchNotificationMessageSenderPhabricator>(
                      id, branchNotificationMessageSenderPhabricator);
                })
            .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    return phabricator;
  }

  private Map<String, BranchNotificationMessageSenderGithub> createGithubInstances(
      BranchNotificationMessageSendersConfigurationProperties
          branchNotificationMessageSendersConfigurationProperties,
      GithubClients githubClients) {
    Map<String, BranchNotificationMessageSenderGithub> githubMessageSenders =
        branchNotificationMessageSendersConfigurationProperties.getGithub().entrySet().stream()
            .map(
                e -> {
                  String id = e.getKey();
                  GithubConfigurationProperties githubConfigurationProperties = e.getValue();
                  GithubClient githubClient =
                      githubClients.getClient(githubConfigurationProperties.getOwner());
                  BranchNotificationMessageSenderGithub branchNotificationMessageSenderGithub =
                      new BranchNotificationMessageSenderGithub(id, githubClient);
                  return new SimpleEntry<String, BranchNotificationMessageSenderGithub>(
                      id, branchNotificationMessageSenderGithub);
                })
            .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    return githubMessageSenders;
  }
}
