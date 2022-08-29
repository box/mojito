package com.box.l10n.mojito.service.branch.notification.slack;

import static org.junit.Assert.assertEquals;

import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.service.branch.BranchUrlBuilder;
import com.box.l10n.mojito.slack.request.Message;
import com.box.l10n.mojito.utils.ServerConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {
      BranchNotificationMessageBuilderSlack.class,
      BranchNotificationCustomMessageBuilderSlackTest.class,
      BranchUrlBuilder.class,
      ServerConfig.class
    },
    properties = {
      "l10n.branchNotification.slack.notification.message.new=Custom message text for new messages stating the time that screenshots need to be uploaded ",
      "l10n.branchNotification.slack.notification.message.updated=Test custom message for update messages stating the screenshot upload deadline ",
      "l10n.branchNotification.slack.notification.message.translationsReady=Custom translations ready text",
      "l10n.branchNotification.slack.notification.message.screenshotsMissing=Custom screenshots missing text :warning:",
      "l10n.branchNotification.slack.notification.message.noMoreStrings=Custom no more string message"
    })
public class BranchNotificationCustomMessageBuilderSlackTest {

  ObjectMapper objectMapper = new ObjectMapper();

  @Autowired BranchNotificationMessageBuilderSlack branchNotificationMessageBuilderSlack;

  @Test
  public void testGetSummaryString() {
    List<String> strings = new ArrayList<>();
    strings.add("string 1");
    strings.add("very long string 2 ---------------------------------------------------------");

    for (int i = 3; i < 30; i++) {
      strings.add("s" + i);
    }

    String summaryString = branchNotificationMessageBuilderSlack.getSummaryString(strings);
    assertEquals(
        "string 1, very long string 2 ------------------..., s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15, s16, s17, s18, s19, s20",
        summaryString);
  }

  @Test
  public void testGetSummaryString1Element() {
    String summaryString =
        branchNotificationMessageBuilderSlack.getSummaryString(Arrays.asList("string 1"));
    assertEquals("string 1", summaryString);
  }

  @Test
  public void testGetNewMessage() {
    Message newMessage =
        branchNotificationMessageBuilderSlack.getNewMessage(
            "channel-test", "pr-test", Arrays.asList("string1", "string2"));
    String json = objectMapper.writeValueAsStringUnchecked(newMessage);
    assertEquals(
        "{\"channel\":\"channel-test\",\"text\":null,\"attachments\":[{\"title\":null,\"text\":\"Custom message text for new messages stating the time "
            + "that screenshots need to be uploaded \",\"fallback\":null,\"color\":\"good\",\"actions\":[{\"type\":\"button\",\"text\":\"Screen"
            + "shots\",\"url\":\"http://localhost:8080/branches?searchText=pr-test&deleted=false&onlyMyBranches=false\",\"style\":\"prim"
            + "ary\"}],\"fields\":[{\"title\":\"PR\",\"value\":\"pr-test\",\"short\":true},{\"title\":\"String number"
            + "\",\"value\":\"2\",\"short\":true},{\"title\":\"Strings\",\"value\":\"string1, string2\",\"short\":null"
            + "}],\"mrkdwn_in\":[\"text\",\"pretex\",\"fields\"]}],\"thread_ts\":null}",
        json);
  }

  @Test
  public void testGetTranslatedMessage() {
    Message newMessage =
        branchNotificationMessageBuilderSlack.getTranslatedMessage("channel-test", "pr-test");
    String json = objectMapper.writeValueAsStringUnchecked(newMessage);
    assertEquals(
        "{\"channel\":\"channel-test\",\"text\":\"Custom translations ready text\",\"attachments\":[],\"thread_ts\":\"pr-test\"}",
        json);
  }

  @Test
  public void testGetScreenshotMissingMessage() {
    Message newMessage =
        branchNotificationMessageBuilderSlack.getScreenshotMissingMessage(
            "channel-test", "pr-test");
    String json = objectMapper.writeValueAsStringUnchecked(newMessage);
    assertEquals(
        "{\"channel\":\"channel-test\",\"text\":\"Custom screenshots missing text :warning:\",\"attachments\":[],\"thread_ts\":\"pr-test\"}",
        json);
  }

  @Test
  public void testGetUpdatedMessage() {
    Message newMessage =
        branchNotificationMessageBuilderSlack.getUpdatedMessage(
            "channel-test", "pr-test", "ts-test", Arrays.asList("string1", "string2"));
    String json = objectMapper.writeValueAsStringUnchecked(newMessage);
    assertEquals(
        "{\"channel\":\"channel-test\",\"text\":null,\"attachments\":[{\"title\":null,\"text\":\"Test custom message for update "
            + "messages stating the screenshot upload deadline \",\"fallback\":null,\"color\":\"good\",\"actions\":[{\"type\":\"button\",\""
            + "text\":\"Screenshots\",\"url\":\"http://localhost:8080/branches?searchText=ts-test&deleted=false&onlyMyBranches=false\",\""
            + "style\":\"primary\"}],\"fields\":[{\"title\":\"PR\",\"value\":\"ts-test\",\"short\":true},{\"title\":\""
            + "String number\",\"value\":\"2\",\"short\":true},{\"title\":\"Strings\",\"value\":\"string1, string2\",\""
            + "short\":null}],\"mrkdwn_in\":[\"text\",\"pretex\",\"fields\"]}],\"thread_ts\":\"pr-test\"}",
        json);
  }

  @Test
  public void testGetUpdatedMessageNoMoreStrings() {
    Message newMessage =
        branchNotificationMessageBuilderSlack.getUpdatedMessage(
            "channel-test", "pr-test", "ts-test", Arrays.asList());
    String json = objectMapper.writeValueAsStringUnchecked(newMessage);
    assertEquals(
        "{\"channel\":\"channel-test\",\"text\":\"Custom no more string message\",\"attachments\":[],\"thread_ts\":\"pr-test\"}",
        json);
  }
}
