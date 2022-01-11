package com.box.l10n.mojito.notifications.service.phabricator;

import com.box.l10n.mojito.notifications.service.NotificationServiceException;
import com.box.l10n.mojito.phabricator.DifferentialRevision;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class PhabricatorNotificationMessageSenderTest {

    @Mock
    DifferentialRevision differentialRevisionMock;

    PhabricatorNotificationMessageSender phabricatorNotificationService;

    ImmutableMap<String, String> paramMap;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        paramMap = ImmutableMap.<String, String>builder().put(PhabricatorParameters.REVISION_ID.getParamKey(), "D12345").build();
        phabricatorNotificationService = new PhabricatorNotificationMessageSender();
        phabricatorNotificationService.differentialRevision = differentialRevisionMock;
    }

    @Test
    public void testNotificationAddedToRevisionAsComment() {
        phabricatorNotificationService.sendMessage("some message", paramMap);
        Mockito.verify(differentialRevisionMock, Mockito.times(1)).addComment("D12345", "some message");
    }

    @Test(expected = NotificationServiceException.class)
    public void testExceptionThrownIfNoRevisionIdProvided() {
        phabricatorNotificationService.sendMessage("a message", ImmutableMap.<String, String>builder().build());
    }
}
