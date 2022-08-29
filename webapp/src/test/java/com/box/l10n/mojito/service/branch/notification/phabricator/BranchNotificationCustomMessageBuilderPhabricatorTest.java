package com.box.l10n.mojito.service.branch.notification.phabricator;

import static org.junit.Assert.assertEquals;

import com.box.l10n.mojito.service.branch.BranchUrlBuilder;
import com.box.l10n.mojito.utils.ServerConfig;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {
      BranchNotificationMessageBuilderPhabricator.class,
      com.box.l10n.mojito.service.branch.notification.phabricator
          .BranchNotificationCustomMessageBuilderPhabricatorTest.class,
      BranchUrlBuilder.class,
      ServerConfig.class
    },
    properties = {
      "l10n.branchNotification.phabricator.notification.message.new=Custom message text for new messages stating the time that screenshots need to be uploaded ",
      "l10n.branchNotification.phabricator.notification.message.updated=Test custom message for update messages stating the screenshot upload deadline ",
      "l10n.branchNotification.phabricator.notification.message.noMoreStrings=Test custom message for no more strings ",
      "l10n.branchNotification.phabricator.notification.message.translationsReady=Test custom message for translated strings ready",
      "l10n.branchNotification.phabricator.notification.message.screenshotsMissing=Test custom message for screenshots missing"
    })
public class BranchNotificationCustomMessageBuilderPhabricatorTest {

  @Autowired
  BranchNotificationMessageBuilderPhabricator branchNotificationMessageBuilderPhabricator;

  @Test
  public void getCustomNewMessage() {
    String newMessage =
        branchNotificationMessageBuilderPhabricator.getNewMessage(
            "branchTest", Arrays.asList("string1", "string2"));
    assertEquals(
        "Custom message text for new messages stating the time that screenshots need to be uploaded "
            + "[→ Go to Mojito](http://localhost:8080/branches?searchText=branchTest&deleted=false&onlyMyBranches=false)\n"
            + "\n"
            + "**Strings:**\n"
            + " - string1\n"
            + " - string2",
        newMessage);
  }

  @Test
  public void getCustomUpdatedMessage() {
    String updatedMessage =
        branchNotificationMessageBuilderPhabricator.getUpdatedMessage(
            "branchTest", Arrays.asList("string1", "string2"));
    assertEquals(
        "Test custom message for update messages stating the screenshot upload deadline "
            + "[→ Go to Mojito](http://localhost:8080/branches?searchText=branchTest&deleted=false&onlyMyBranches=false)\n"
            + "\n"
            + "**Strings:**\n"
            + " - string1\n"
            + " - string2",
        updatedMessage);
  }

  @Test
  public void getCustomNoMoreStringsMessage() {
    String noMoreStrings = branchNotificationMessageBuilderPhabricator.getNoMoreStringsMessage();
    assertEquals("Test custom message for no more strings ", noMoreStrings);
  }

  @Test
  public void getCustomTranslationsReadyMessage() {
    String translationsReady = branchNotificationMessageBuilderPhabricator.getTranslatedMessage();
    assertEquals("Test custom message for translated strings ready", translationsReady);
  }

  @Test
  public void getCustomScreenshotsMissingMessage() {
    String screenshotsMissingMsg =
        branchNotificationMessageBuilderPhabricator.getScreenshotMissingMessage();
    assertEquals("Test custom message for screenshots missing", screenshotsMissingMsg);
  }
}
