package com.box.l10n.mojito.service.branch.notification.slack;

import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.service.branch.BranchUrlBuilder;
import com.box.l10n.mojito.service.branch.notification.phabricator.BranchNotificationMessageBuilderPhabricator;
import com.box.l10n.mojito.service.branch.notification.phabricator.BranchNotificationMessageBuilderPhabricatorTest;
import com.box.l10n.mojito.slack.request.Message;
import com.box.l10n.mojito.utils.ServerConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {
        BranchNotificationMessageBuilderSlack.class,
        BranchNotificationMessageBuilderSlackTest.class,
        BranchUrlBuilder.class,
        ServerConfig.class})
@EnableAutoConfiguration
@IntegrationTest("spring.datasource.initialize=false")
public class BranchNotificationMessageBuilderSlackTest {

    ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    BranchNotificationMessageBuilderSlack branchNotificationMessageBuilderSlack;

    @Test
    public void testGetSummaryString() {
        String summaryString = branchNotificationMessageBuilderSlack.getSummaryString(Arrays.asList(
                "string 1",
                "very long string 2 ---------------------------------------------------------",
                "string 3",
                "string 4",
                "string 5",
                "string 6"));

        assertEquals("string 1, very long string 2 ------------------..., string 3", summaryString);
    }

    @Test
    public void testGetSummaryString1Element() {
        String summaryString = branchNotificationMessageBuilderSlack.getSummaryString(Arrays.asList("string 1"));
        assertEquals("string 1", summaryString);
    }

    @Test
    public void testGetNewMessage() {
        Message newMessage = branchNotificationMessageBuilderSlack.getNewMessage("channel-test", "pr-test", Arrays.asList("string1", "string2"));
        String json = objectMapper.writeValueAsStringUnsafe(newMessage);
        assertEquals("{\"channel\":\"channel-test\",\"text\":null,\"attachments\":[{\"title\":null,\"text\":\"W" +
                "e received your strings! Please *add screenshots* as soon as possible and *wait for translations* befor" +
                "e releasing.\",\"fallback\":null,\"color\":\"good\",\"actions\":[{\"type\":\"button\",\"text\":\"Screen" +
                "shots\",\"url\":\"http://localhost:8080/branches?searchText=pr-test&deleted=false\",\"style\":\"prim" +
                "ary\"}],\"fields\":[{\"title\":\"PR\",\"value\":\"pr-test\",\"short\":true},{\"title\":\"String number" +
                "\",\"value\":\"2\",\"short\":true},{\"title\":\"Strings\",\"value\":\"string1, string2\",\"short\":null" +
                "}],\"mrkdwn_in\":[\"text\",\"pretex\",\"fields\"]}],\"thread_ts\":null}", json);
    }

    @Test
    public void testGetTranslatedMessage() {
        Message newMessage = branchNotificationMessageBuilderSlack.getTranslatedMessage("channel-test", "pr-test");
        String json = objectMapper.writeValueAsStringUnsafe(newMessage);
        assertEquals("{\"channel\":\"channel-test\",\"text\":\"Translations are ready !! :party:\",\"attachments\":[],\"thread_ts\":\"pr-test\"}", json);
    }

    @Test
    public void testGetScreenshotMissingMessage() {
        Message newMessage = branchNotificationMessageBuilderSlack.getScreenshotMissingMessage("channel-test", "pr-test");
        String json = objectMapper.writeValueAsStringUnsafe(newMessage);
        assertEquals("{\"channel\":\"channel-test\",\"text\":\":warning: Please provide screenshots to help localization team :warning:\",\"attachments\":[],\"thread_ts\":\"pr-test\"}", json);
    }

    @Test
    public void testGetUpdatedMessage() {
        Message newMessage = branchNotificationMessageBuilderSlack.getUpdatedMessage("channel-test", "pr-test", "ts-test", Arrays.asList("string1", "string2"));
        String json = objectMapper.writeValueAsStringUnsafe(newMessage);
        assertEquals("{\"channel\":\"channel-test\",\"text\":null,\"attachments\":[{\"title\":null,\"text\":\"Y" +
                "our branch was updated with new strings! Please *add screenshots* as soon as possible and *wait for tra" +
                "nslations* before releasing.\",\"fallback\":null,\"color\":\"good\",\"actions\":[{\"type\":\"button\",\"" +
                "text\":\"Screenshots\",\"url\":\"http://localhost:8080/branches?searchText=ts-test&deleted=false\",\"" +
                "style\":\"primary\"}],\"fields\":[{\"title\":\"PR\",\"value\":\"ts-test\",\"short\":true},{\"title\":\"" +
                "String number\",\"value\":\"2\",\"short\":true},{\"title\":\"Strings\",\"value\":\"string1, string2\",\"" +
                "short\":null}],\"mrkdwn_in\":[\"text\",\"pretex\",\"fields\"]}],\"thread_ts\":\"pr-test\"}", json);
    }

}