package com.box.l10n.mojito.notifications.service.slack;

import com.box.l10n.mojito.notifications.service.NotificationServiceException;
import com.box.l10n.mojito.slack.request.Message;
import com.box.l10n.mojito.slack.SlackClient;
import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class SlackNotificationMessageSenderTest {

    @Mock
    private SlackClient slackClientMock;

    @Captor
    private ArgumentCaptor<Message> messageCaptor;

    private SlackNotificationMessageSender slackNotificationService;

    private ImmutableMap<String, String> serviceMap;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        slackNotificationService = new SlackNotificationMessageSender();
        slackNotificationService.slackClient = slackClientMock;
        serviceMap = ImmutableMap.<String, String>builder().put(SlackParameters.USERNAME.getParamKey(), "username").build();
    }

    @Test
    public void testNotificationSentToSlackClient() throws Exception {
        slackNotificationService.sendMessage("a message", serviceMap);
        Mockito.verify(slackClientMock, Mockito.times(1)).sendInstantMessage(messageCaptor.capture());
        Message slackMessage = messageCaptor.getValue();
        Assert.assertTrue(slackMessage.getAttachments().size() == 1);
        Assert.assertTrue(slackMessage.getAttachments().get(0).getText().equals("a message"));
    }

    @Test
    public void testColorChangeOfAttachment() throws Exception {
        ImmutableMap<String, String> paramMap = ImmutableMap.<String, String>builder()
                .put(SlackParameters.USERNAME.getParamKey(), "username")
                .put(SlackParameters.ATTACHMENT_COLOR.getParamKey(), "error")
                .build();
        slackNotificationService.sendMessage("a message", paramMap);
        Mockito.verify(slackClientMock, Mockito.times(1)).sendInstantMessage(messageCaptor.capture());
        Message slackMessage = messageCaptor.getValue();
        Assert.assertTrue(slackMessage.getAttachments().get(0).getText().equals("a message"));
        Assert.assertTrue(slackMessage.getAttachments().get(0).getColor().equals("error"));
    }


    @Test(expected = NotificationServiceException.class)
    public void testExceptionThrownIfUsernameNotProvided() throws Exception {
        slackNotificationService.sendMessage("some message", ImmutableMap.<String, String>builder().build());
    }
}
