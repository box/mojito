package com.box.l10n.mojito.service.branch.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.github.GithubClients;
import com.box.l10n.mojito.slack.SlackClient;
import com.box.l10n.mojito.slack.SlackClients;
import org.junit.Test;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class BranchNotificationMessageSendersTest {

  @Test
  public void noop() {
    BranchNotificationMessageSendersConfigurationProperties config =
        getTestBranchNotificationMessageSendersConfigurationProperties(
            "l10n.branchNotification.notifiers.noop.noop-1.enabled=true");
    BranchNotificationMessageSenders branchNotificationMessageSenders =
        new BranchNotificationMessageSenders(config, null, null, null);
    assertThat(branchNotificationMessageSenders.getById("noop-1")).isNotNull();
  }

  @Test
  public void noopInvalidEnabled() {
    BranchNotificationMessageSendersConfigurationProperties config =
        getTestBranchNotificationMessageSendersConfigurationProperties(
            "l10n.branchNotification.notifiers.noop.noop-1.enabled=false");

    assertThatThrownBy(() -> new BranchNotificationMessageSenders(config, null, null, null))
        .hasMessage("only enabled=true is accepted value");
  }

  @Test
  public void noopInvalidId() {
    BranchNotificationMessageSendersConfigurationProperties config =
        getTestBranchNotificationMessageSendersConfigurationProperties(
            "l10n.branchNotification.notifiers.noop.badid-1.enabled=true");
    assertThatThrownBy(() -> new BranchNotificationMessageSenders(config, null, null, null))
        .hasMessage("name must start with prefix: noop-");
  }

  @Test
  public void slack() {
    BranchNotificationMessageSendersConfigurationProperties config =
        getTestBranchNotificationMessageSendersConfigurationProperties(
            "l10n.branchNotification.notifiers.slack.slack-1.slackClientId=slackClientId1",
            "l10n.branchNotification.notifiers.slack.slack-1.userEmailPattern={0}@mojito.org");

    final SlackClients slackClients = mock(SlackClients.class);
    final SlackClient slackClient = mock(SlackClient.class);
    when(slackClients.getById("slackClientId1")).thenReturn(slackClient);

    BranchNotificationMessageSenders branchNotificationMessageSenders =
        new BranchNotificationMessageSenders(config, null, slackClients, null);
    assertThat(branchNotificationMessageSenders.getById("slack-1")).isNotNull();

    verify(slackClients, times(1)).getById("slackClientId1");
  }

  @Test
  public void slackWithMessageOverride() {
    BranchNotificationMessageSendersConfigurationProperties config =
        getTestBranchNotificationMessageSendersConfigurationProperties(
            "l10n.branchNotification.notifiers.slack.slack-1.slackClientId=slackClientId1",
            "l10n.branchNotification.notifiers.slack.slack-1.userEmailPattern={0}@mojito.org",
            "l10n.branchNotification.notifiers.slack.slack-1.messages.newStrings=newString override");

    final SlackClients slackClients = mock(SlackClients.class);
    final SlackClient slackClient = mock(SlackClient.class);
    when(slackClients.getById("slackClientId1")).thenReturn(slackClient);

    BranchNotificationMessageSenders branchNotificationMessageSenders =
        new BranchNotificationMessageSenders(config, null, slackClients, null);
    assertThat(branchNotificationMessageSenders.getById("slack-1")).isNotNull();

    verify(slackClients, times(1)).getById("slackClientId1");
  }

  @Test
  public void slackInvalidId() {
    BranchNotificationMessageSendersConfigurationProperties config =
        getTestBranchNotificationMessageSendersConfigurationProperties(
            "l10n.branchNotification.notifiers.slack.badid-1.slackClientId=slackClientId1");
    assertThatThrownBy(() -> new BranchNotificationMessageSenders(config, null, null, null))
        .hasMessage("name must start with prefix: slack-");
  }

  @Test
  public void phabricator() {
    BranchNotificationMessageSendersConfigurationProperties config =
        getTestBranchNotificationMessageSendersConfigurationProperties(
            "l10n.branchNotification.notifiers.phabricator.phabricator-1.url=https://phabricator.pinadmin.com",
            "l10n.branchNotification.notifiers.phabricator.phabricator-1.token=token-value",
            "l10n.branchNotification.notifiers.phabricator.phabricator-1.reviewer=reviewer-value");
    BranchNotificationMessageSenders branchNotificationMessageSenders =
        new BranchNotificationMessageSenders(config, null, null, null);
    assertThat(branchNotificationMessageSenders.getById("phabricator-1")).isNotNull();
  }

  @Test
  public void phabricatorWithMessageOverride() {
    BranchNotificationMessageSendersConfigurationProperties config =
        getTestBranchNotificationMessageSendersConfigurationProperties(
            "l10n.branchNotification.notifiers.phabricator.phabricator-1.url=https://phabricator.pinadmin.com",
            "l10n.branchNotification.notifiers.phabricator.phabricator-1.token=token-value",
            "l10n.branchNotification.notifiers.phabricator.phabricator-1.reviewer=reviewer-value",
            "l10n.branchNotification.notifiers.phabricator.phabricator-1.messages.newStrings=newStrings override");
    BranchNotificationMessageSenders branchNotificationMessageSenders =
        new BranchNotificationMessageSenders(config, null, null, null);
    assertThat(branchNotificationMessageSenders.getById("phabricator-1")).isNotNull();
  }

  @Test
  public void phabricatorInvalidId() {
    BranchNotificationMessageSendersConfigurationProperties config =
        getTestBranchNotificationMessageSendersConfigurationProperties(
            "l10n.branchNotification.notifiers.phabricator.badid-1.url=false");
    assertThatThrownBy(() -> new BranchNotificationMessageSenders(config, null, null, null))
        .hasMessage("name must start with prefix: phabricator-");
  }

  @Test
  public void github() {
    BranchNotificationMessageSendersConfigurationProperties config =
        getTestBranchNotificationMessageSendersConfigurationProperties(
            "l10n.branchNotification.notifiers.github.github-1.owner=owner1");
    GithubClients githubClients = mock(GithubClients.class);
    BranchNotificationMessageSenders branchNotificationMessageSenders =
        new BranchNotificationMessageSenders(config, githubClients, null, null);
    assertThat(branchNotificationMessageSenders.getById("github-1")).isNotNull();
    verify(githubClients, times(1)).getClient("owner1");
  }

  @Test
  public void githubInvalidId() {
    BranchNotificationMessageSendersConfigurationProperties config =
        getTestBranchNotificationMessageSendersConfigurationProperties(
            "l10n.branchNotification.notifiers.github.badid-1.owner=owner1");
    assertThatThrownBy(() -> new BranchNotificationMessageSenders(config, null, null, null))
        .hasMessage("name must start with prefix: github-");
  }

  private static BranchNotificationMessageSendersConfigurationProperties
      getTestBranchNotificationMessageSendersConfigurationProperties(String... pairs) {
    AnnotationConfigApplicationContext annotationConfigApplicationContext =
        new AnnotationConfigApplicationContext();
    annotationConfigApplicationContext.register(
        BranchNotificationMessageSendersConfigurationProperties.class,
        ConfigurationPropertiesAutoConfiguration.class);
    TestPropertyValues.of(pairs).applyTo(annotationConfigApplicationContext);
    annotationConfigApplicationContext.refresh();
    BranchNotificationMessageSendersConfigurationProperties config =
        annotationConfigApplicationContext.getBean(
            BranchNotificationMessageSendersConfigurationProperties.class);
    return config;
  }
}
