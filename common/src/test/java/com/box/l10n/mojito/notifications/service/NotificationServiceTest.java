package com.box.l10n.mojito.notifications.service;

import com.box.l10n.mojito.notifications.service.phabricator.PhabricatorNotificationMessageSender;
import com.box.l10n.mojito.notifications.service.slack.SlackNotificationMessageSender;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.doReturn;

public class NotificationServiceTest {

    NotificationService notificationService;

    @Mock
    PhabricatorNotificationMessageSender phabricatorNotificationServiceMock;

    @Mock
    SlackNotificationMessageSender slackNotificationServiceMock;

    ImmutableMap emptyMap = ImmutableMap.<String, String>builder().build();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        notificationService = new NotificationService();
        List<ThirdPartyNotificationMessageSender> notifyServices = new ArrayList<>();
        notifyServices.add(phabricatorNotificationServiceMock);
        notifyServices.add(slackNotificationServiceMock);
        notificationService.messageSenders = notifyServices;
    }

    @Test
    public void testNotificationServicesCalled() throws Exception {
        notificationService.sendNotifications("some message", ImmutableSet.<ThirdPartyNotificationType>builder().add(ThirdPartyNotificationType.PHABRICATOR).add(ThirdPartyNotificationType.SLACK).build(), emptyMap);
        Mockito.verify(phabricatorNotificationServiceMock, Mockito.times(1)).sendMessage("some message", emptyMap);
        Mockito.verify(slackNotificationServiceMock, Mockito.times(1)).sendMessage("some message", emptyMap);
    }

    @Test
    public void testPhabricatorCalledIfInSet() throws Exception {
        notificationService.sendNotifications("some message", ImmutableSet.<ThirdPartyNotificationType>builder().add(ThirdPartyNotificationType.PHABRICATOR).build(), emptyMap);
        Mockito.verify(phabricatorNotificationServiceMock, Mockito.times(1)).sendMessage("some message", emptyMap);
        Mockito.verify(slackNotificationServiceMock, Mockito.times(0)).sendMessage("some message", emptyMap);
    }

    @Test
    public void testSlackCalledIfInSet() throws Exception {
        notificationService.sendNotifications("some message", ImmutableSet.<ThirdPartyNotificationType>builder().add(ThirdPartyNotificationType.SLACK).build(), emptyMap);
        Mockito.verify(phabricatorNotificationServiceMock, Mockito.times(0)).sendMessage("some message", emptyMap);
        Mockito.verify(slackNotificationServiceMock, Mockito.times(1)).sendMessage("some message", emptyMap);
    }

    @Test(expected = NotificationServiceException.class)
    public void testExceptionThrownIfPhabricatorRequestedButNotConfigured() {
        List<ThirdPartyNotificationMessageSender> notificationServices = new ArrayList<>();
        notificationServices.add(slackNotificationServiceMock);
        notificationService.messageSenders = notificationServices;
        notificationService.sendNotifications("some message", ImmutableSet.<ThirdPartyNotificationType>builder().add(ThirdPartyNotificationType.PHABRICATOR).build(), emptyMap);
    }

    @Test(expected = NotificationServiceException.class)
    public void testExceptionThrownIfSlackRequestedButNotConfigured() {
        List<ThirdPartyNotificationMessageSender> notificationServices = new ArrayList<>();
        notificationServices.add(phabricatorNotificationServiceMock);
        notificationService.messageSenders = notificationServices;
        notificationService.sendNotifications("some message", ImmutableSet.<ThirdPartyNotificationType>builder().add(ThirdPartyNotificationType.SLACK).build(), emptyMap);
    }
}
