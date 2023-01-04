package com.box.l10n.mojito.service.branch.notification;

import com.box.l10n.mojito.service.branch.notification.BranchNotificationMessageSendersConfigurationProperties.GithubConfigurationProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {BranchNotificationMessageSendersConfigurationProperties.class},
    properties = {
      "l10n.branchNotification.notifiers.noop.noop-1.attr1=attr1val1",
      "l10n.branchNotification.notifiers.github.github-enterprise.owner=testOwner1",
      "l10n.branchNotification.notifiers.github.github-public.owner=testOwner2",
      "l10n.branchNotification.notifiers.slack.slack-default.slackClientId=slackClientId1",
      "l10n.branchNotification.notifiers.slack.slack-default.messages.newStrings=new strings override",
      "l10n.branchNotification.notifiers.slack.slack-default.messages.updatedStrings=updated strings override",
    })
@EnableConfigurationProperties
public class BranchNotificationMessageSendersConfigurationPropertiesTest {

  @Autowired
  BranchNotificationMessageSendersConfigurationProperties
      branchNotificationMessageSendersConfigurationProperties;

  @Test
  public void readConfiguration() {
    final Map<String, GithubConfigurationProperties> github =
        branchNotificationMessageSendersConfigurationProperties.getGithub();
    System.out.println(github);
  }
}
