package com.box.l10n.mojito.service.branch.notification;

import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import java.util.Arrays;
import org.assertj.core.api.Assertions;
import org.junit.Assume;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class BranchNotificationMessageSendersITest extends ServiceTestBase {

  @Autowired BranchNotificationMessageSenders branchNotificationMessageSenders;

  @Test
  public void noop() throws Exception {
    // noop-1 is the default test notifier, defined in application-test.properties
    BranchNotificationMessageSender noop = branchNotificationMessageSenders.getById("noop-1");
    Assertions.assertThat(noop).isNotNull();
  }

  @Test
  public void github() throws Exception, BranchNotificationMessageSenderException {

    String notifierId = "github-1";
    String branchName = "org/repo/pull/1234";
    String username = "username";

    BranchNotificationMessageSender github = branchNotificationMessageSenders.getById(notifierId);

    Assume.assumeNotNull(github);

    String messageId =
        github.sendNewMessage(
            branchName, username, Arrays.asList("test 1 string", "test 2 string"));

    github.sendUpdatedMessage(
        branchName, username, messageId, Arrays.asList("test 1 string", "test 2 string"));

    github.sendScreenshotMissingMessage(branchName, messageId, username);
    github.sendTranslatedMessage(branchName, username, messageId, null);
  }

  @Test
  public void slack() throws Exception, BranchNotificationMessageSenderException {

    String notifierId = "slack-1";
    String branchName = "org/repo/pull/1234";
    String username = "username";

    BranchNotificationMessageSender slack = branchNotificationMessageSenders.getById(notifierId);

    Assume.assumeNotNull(slack);

    String messageId =
        slack.sendNewMessage(branchName, username, Arrays.asList("test 1 string", "test 2 string"));

    slack.sendUpdatedMessage(
        branchName, username, messageId, Arrays.asList("test 1 string", "test 2 string"));

    slack.sendScreenshotMissingMessage(branchName, messageId, username);
    slack.sendTranslatedMessage(branchName, username, messageId, null);
  }
}
