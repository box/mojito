package com.box.l10n.mojito.notifications.service.phabricator;

import com.box.l10n.mojito.notifications.service.NotificationServiceException;
import com.box.l10n.mojito.notifications.service.ThirdPartyNotificationMessageSender;
import com.box.l10n.mojito.phabricator.DifferentialRevision;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Sends notifications as comments on a Phabricator differential revision using {@link DifferentialRevision}.
 *
 * @author mallen
 */
@ConditionalOnProperty("l10n.notifications.phabricator.enabled")
@Component
public class PhabricatorNotificationMessageSender extends ThirdPartyNotificationMessageSender {

    @Autowired
    DifferentialRevision differentialRevision;

    @Override
    public void sendMessage(String message, ImmutableMap<String, String> serviceParameters) {
        checkParameters(serviceParameters);
        differentialRevision.addComment(serviceParameters.get(PhabricatorParameters.REVISION_ID.getParamKey()), message);
    }

    private void checkParameters(ImmutableMap<String, String> parameters) {
        if (!parameters.containsKey(PhabricatorParameters.REVISION_ID.getParamKey())) {
            throw new NotificationServiceException("Phabricator revision id must be provided when sending notifications to a Phabricator revision.");
        }
    }
}
