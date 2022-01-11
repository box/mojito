package com.box.l10n.mojito.notifications.service;

import com.box.l10n.mojito.notifications.service.phabricator.PhabricatorNotificationMessageSender;
import com.box.l10n.mojito.notifications.service.slack.SlackNotificationMessageSender;

import java.util.Arrays;

public enum ThirdPartyNotificationType {

    PHABRICATOR(PhabricatorNotificationMessageSender.class),
    SLACK(SlackNotificationMessageSender.class);

    Class<? extends ThirdPartyNotificationMessageSender> notificationServiceClass;

    ThirdPartyNotificationType(Class<? extends ThirdPartyNotificationMessageSender> notificationServiceClass) {
        this.notificationServiceClass = notificationServiceClass;
    }

    public Class<? extends ThirdPartyNotificationMessageSender> getNotificationMessageSenderClass() {
        return notificationServiceClass;
    }

    public static ThirdPartyNotificationType getThirdPartyNotificationServiceType(ThirdPartyNotificationMessageSender messageSender) {
        return Arrays.stream(ThirdPartyNotificationType.values())
                .filter(type -> messageSender.getClass().equals(type.getNotificationMessageSenderClass()))
                .findFirst().orElseThrow(() -> new NotificationServiceException("No matching ThirdPartyNotificationType for '" + messageSender.getClass().getName() + "'"));
    }
}
