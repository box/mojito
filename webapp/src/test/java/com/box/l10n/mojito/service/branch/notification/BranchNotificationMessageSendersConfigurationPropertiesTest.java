package com.box.l10n.mojito.service.branch.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.box.l10n.mojito.service.branch.notification.BranchNotificationMessageSendersConfigurationProperties.NoopConfigurationProperties;
import com.box.l10n.mojito.service.branch.notification.BranchNotificationMessageSendersConfigurationProperties.PhabricatorConfigurationProperties;
import com.box.l10n.mojito.service.branch.notification.BranchNotificationMessageSendersConfigurationProperties.SlackConfigurationProperties;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {BranchNotificationMessageSendersConfigurationProperties.class},
    properties = {
      "l10n.branchNotification.notifiers.noop.noop-1.enabled=true",
      "l10n.branchNotification.notifiers.slack.slack-1.slackClientId=slackClientId1",
      "l10n.branchNotification.notifiers.slack.slack-1.userEmailPattern={0}@test.com",
      "l10n.branchNotification.notifiers.slack.slack-1.messages.newStrings=Override newStrings",
      "l10n.branchNotification.notifiers.phabricator.phabricator-1.url=url1",
      "l10n.branchNotification.notifiers.phabricator.phabricator-1.token=token1",
      "l10n.branchNotification.notifiers.phabricator.phabricator-1.reviewer=reviewer1",
      "l10n.branchNotification.notifiers.phabricator.phabricator-1.messages.newStrings=Override newStrings Phabricator"
    })
@EnableConfigurationProperties
public class BranchNotificationMessageSendersConfigurationPropertiesTest {

  @Autowired
  BranchNotificationMessageSendersConfigurationProperties
      branchNotificationMessageSendersConfigurationProperties;

  @Test
  public void noop() {
    final Map<String, NoopConfigurationProperties> noop =
        branchNotificationMessageSendersConfigurationProperties.getNoop();
    assertThat(noop).containsOnlyKeys("noop-1");
    assertThat(noop.get("noop-1").isEnabled()).isTrue();
  }

  @Test
  public void slack() {
    final Map<String, SlackConfigurationProperties> slack =
        branchNotificationMessageSendersConfigurationProperties.getSlack();
    assertThat(slack).containsOnlyKeys("slack-1");

    SlackConfigurationProperties slack1 = slack.get("slack-1");
    assertThat(slack1)
        .extracting("slackClientId", "userEmailPattern", "messages.newStrings")
        .containsExactly("slackClientId1", "{0}@test.com", "Override newStrings");
  }

  @Test
  public void phabricator() {
    final Map<String, PhabricatorConfigurationProperties> phabricator =
        branchNotificationMessageSendersConfigurationProperties.getPhabricator();
    assertThat(phabricator).containsOnlyKeys("phabricator-1");

    PhabricatorConfigurationProperties phabricator1 = phabricator.get("phabricator-1");
    assertThat(phabricator1)
        .extracting("url", "token", "reviewer", "messages.newStrings")
        .containsExactly("url1", "token1", "reviewer1", "Override newStrings Phabricator");
  }
}
