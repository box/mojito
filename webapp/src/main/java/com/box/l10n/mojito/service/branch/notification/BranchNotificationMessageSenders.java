package com.box.l10n.mojito.service.branch.notification;

import com.box.l10n.mojito.github.GithubClient;
import com.box.l10n.mojito.github.GithubClientsFactory;
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
      GithubClientsFactory githubClientsFactory,
      SlackClients slackClients,
      BranchUrlBuilder branchUrlBuilder) {
    this.mapIdToBranchNotificationMessageSender =
        createInstancesFromConfiguration(
            branchNotificationMessageSendersConfigurationProperties,
            githubClientsFactory,
            slackClients,
            branchUrlBuilder);
  }

  public BranchNotificationMessageSender getById(String notifierId) {
    return mapIdToBranchNotificationMessageSender.get(notifierId);
  }

  private Map<String, BranchNotificationMessageSender> createInstancesFromConfiguration(
      BranchNotificationMessageSendersConfigurationProperties
          branchNotificationMessageSendersConfigurationProperties,
      GithubClientsFactory githubClientsFactory,
      SlackClients slackClients,
      BranchUrlBuilder branchUrlBuilder) {

    checkNotifierIdsStartWithPrefix(
        branchNotificationMessageSendersConfigurationProperties.getGithub().keySet(), "github");

    checkNotifierIdsStartWithPrefix(
        branchNotificationMessageSendersConfigurationProperties.getSlack().keySet(), "slack");

    checkNotifierIdsStartWithPrefix(
        branchNotificationMessageSendersConfigurationProperties.getPhabricator().keySet(),
        "phabricator");

    checkNotifierIdsStartWithPrefix(
        branchNotificationMessageSendersConfigurationProperties.getNoop().keySet(), "noop");

    Map<String, BranchNotificationMessageSender> mapIdToBranchNotificationMessageSender =
        new HashMap<>();
    mapIdToBranchNotificationMessageSender.putAll(
        createGithubInstances(
            branchNotificationMessageSendersConfigurationProperties, githubClientsFactory));
    mapIdToBranchNotificationMessageSender.putAll(
        createSlackInstances(
            branchNotificationMessageSendersConfigurationProperties,
            slackClients,
            branchUrlBuilder));
    mapIdToBranchNotificationMessageSender.putAll(
        createPhabricatorInstances(
            branchNotificationMessageSendersConfigurationProperties, branchUrlBuilder));
    mapIdToBranchNotificationMessageSender.putAll(
        createNoopInstances(branchNotificationMessageSendersConfigurationProperties));
    return mapIdToBranchNotificationMessageSender;
  }

  private void checkNotifierIdsStartWithPrefix(
      Set<String> branchNotificationMessageSendersConfigurationProperties, String prefix) {
    String prefixWithDash = prefix + "-";
    if (!branchNotificationMessageSendersConfigurationProperties.stream()
        .allMatch(key -> key.startsWith(prefixWithDash))) {
      throw new RuntimeException("name must start with prefix: " + prefixWithDash);
    }
  }

  private Map<String, BranchNotificationMessageSenderSlack> createSlackInstances(
      BranchNotificationMessageSendersConfigurationProperties
          branchNotificationMessageSendersConfigurationProperties,
      SlackClients slackClients,
      BranchUrlBuilder branchUrlBuilder) {
    Map<String, BranchNotificationMessageSenderSlack> slackNotifiers =
        branchNotificationMessageSendersConfigurationProperties.getSlack().entrySet().stream()
            .map(
                e -> {
                  String notifierId = e.getKey();
                  SlackConfigurationProperties slackConfigurationProperties = e.getValue();
                  SlackClient slackClient =
                      slackClients.getById(slackConfigurationProperties.getSlackClientId());
                  // TODO(jean) SlackChannels should probably refactored?
                  SlackChannels slackChannels = new SlackChannels(slackClient);

                  MessageBuilderConfigurationProperties messageBuilderConfigurationProperties =
                      slackConfigurationProperties.getMessages();
                  BranchNotificationMessageBuilderSlack branchNotificationMessageBuilderSlack =
                      new BranchNotificationMessageBuilderSlack(
                          branchUrlBuilder,
                          messageBuilderConfigurationProperties.getNewStrings(),
                          messageBuilderConfigurationProperties.getUpdatedStrings(),
                          messageBuilderConfigurationProperties.getTranslationsReady(),
                          messageBuilderConfigurationProperties.getScreenshotsMissing(),
                          messageBuilderConfigurationProperties.getNoMoreStrings());

                  BranchNotificationMessageSenderSlack branchNotificationMessageSenderSlack =
                      new BranchNotificationMessageSenderSlack(
                          notifierId,
                          slackClient,
                          slackChannels,
                          branchNotificationMessageBuilderSlack,
                          slackConfigurationProperties.getUserEmailPattern(),
                          slackConfigurationProperties.isUseDirectMessage());

                  return new SimpleEntry<String, BranchNotificationMessageSenderSlack>(
                      notifierId, branchNotificationMessageSenderSlack);
                })
            .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    return slackNotifiers;
  }

  private Map<String, BranchNotificationMessageSenderGithub> createGithubInstances(
      BranchNotificationMessageSendersConfigurationProperties
          branchNotificationMessageSendersConfigurationProperties,
      GithubClientsFactory githubClientsFactory) {
    Map<String, BranchNotificationMessageSenderGithub> githubNotifiers =
        branchNotificationMessageSendersConfigurationProperties.getGithub().entrySet().stream()
            .map(
                e -> {
                  String notifierId = e.getKey();
                  GithubConfigurationProperties githubConfigurationProperties = e.getValue();
                  GithubClient githubClient =
                      githubClientsFactory.getClient(githubConfigurationProperties.getOwner());
                  BranchNotificationMessageSenderGithub branchNotificationMessageSenderGithub =
                      new BranchNotificationMessageSenderGithub(notifierId, githubClient);
                  return new SimpleEntry<String, BranchNotificationMessageSenderGithub>(
                      notifierId, branchNotificationMessageSenderGithub);
                })
            .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    return githubNotifiers;
  }

  private Map<String, BranchNotificationMessageSenderPhabricator> createPhabricatorInstances(
      BranchNotificationMessageSendersConfigurationProperties
          branchNotificationMessageSendersConfigurationProperties,
      BranchUrlBuilder branchUrlBuilder) {
    Map<String, BranchNotificationMessageSenderPhabricator> phabricatorMessageSenders =
        branchNotificationMessageSendersConfigurationProperties.getPhabricator().entrySet().stream()
            .map(
                e -> {
                  String notifierId = e.getKey();
                  PhabricatorConfigurationProperties phabricatorConfigurationProperties =
                      e.getValue();

                  PhabricatorHttpClient phabricatorHttpClient =
                      new PhabricatorHttpClient(
                          phabricatorConfigurationProperties.getUrl(),
                          phabricatorConfigurationProperties.getToken());

                  DifferentialRevision differentialRevision =
                      new DifferentialRevision(phabricatorHttpClient);

                  PhabricatorConfigurationProperties.MessageBuilderConfigurationProperties
                      messageBuilderConfigurationProperties =
                          phabricatorConfigurationProperties
                              .getMessageBuilderConfigurationProperties();

                  BranchNotificationMessageBuilderPhabricator
                      branchNotificationMessageBuilderPhabricator =
                          new BranchNotificationMessageBuilderPhabricator(
                              branchUrlBuilder,
                              messageBuilderConfigurationProperties.getNewNotificationMsgFormat(),
                              messageBuilderConfigurationProperties
                                  .getUpdatedNotificationMsgFormat(),
                              messageBuilderConfigurationProperties.getNewNotificationMsg(),
                              messageBuilderConfigurationProperties.getUpdatedNotificationMsg(),
                              messageBuilderConfigurationProperties.getNoMoreStringsMsg(),
                              messageBuilderConfigurationProperties.getTranslationsReadyMsg(),
                              messageBuilderConfigurationProperties.getScreenshotsMissingMsg());

                  BranchNotificationMessageSenderPhabricator
                      branchNotificationMessageSenderPhabricator =
                          new BranchNotificationMessageSenderPhabricator(
                              notifierId,
                              differentialRevision,
                              phabricatorConfigurationProperties.getReviewer(),
                              phabricatorConfigurationProperties.isBlockingReview(),
                              branchNotificationMessageBuilderPhabricator);
                  return new SimpleEntry<String, BranchNotificationMessageSenderPhabricator>(
                      notifierId, branchNotificationMessageSenderPhabricator);
                })
            .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    return phabricatorMessageSenders;
  }

  private Map<String, BranchNotificationMessageSenderNoop> createNoopInstances(
      BranchNotificationMessageSendersConfigurationProperties
          branchNotificationMessageSendersConfigurationProperties) {
    Map<String, BranchNotificationMessageSenderNoop> noopNotifiers =
        branchNotificationMessageSendersConfigurationProperties.getNoop().entrySet().stream()
            .map(
                e -> {
                  String notifierId = e.getKey();
                  NoopConfigurationProperties noopConfigurationProperties = e.getValue();
                  BranchNotificationMessageSenderNoop branchNotificationMessageSenderNoop =
                      new BranchNotificationMessageSenderNoop(notifierId);
                  return new SimpleEntry<String, BranchNotificationMessageSenderNoop>(
                      notifierId, branchNotificationMessageSenderNoop);
                })
            .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    return noopNotifiers;
  }
}
