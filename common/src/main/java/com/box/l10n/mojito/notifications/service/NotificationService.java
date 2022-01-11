package com.box.l10n.mojito.notifications.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.box.l10n.mojito.notifications.service.ThirdPartyNotificationType.getThirdPartyNotificationServiceType;

/**
 * Notification service that sends messages to third party service providers.
 *
 * @author mallen
 */
@Component
public class NotificationService {

    static Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired(required = false)
    List<ThirdPartyNotificationMessageSender> messageSenders;

    public void sendNotifications(String message, ImmutableSet<ThirdPartyNotificationType> notifyServiceTypes, ImmutableMap<String, String> serviceParameters) throws NotificationServiceException {
        for (ThirdPartyNotificationType service : notifyServiceTypes) {
            if (isServiceConfigured(service)) {
                sendMessageToService(message, serviceParameters, service);
            } else {
                throw new NotificationServiceException(service.name() + " is not configured.");
            }
        }
    }

    private void sendMessageToService(String message, ImmutableMap<String, String> serviceParameters, ThirdPartyNotificationType messageSenderType) {
        messageSenders.stream()
                .filter(messageSender -> messageSenderType.getNotificationMessageSenderClass().isInstance(messageSender))
                .forEach(messageSender -> sendMessage(message, serviceParameters, messageSender));
    }

    private void sendMessage(String message, ImmutableMap<String, String> serviceParameters, ThirdPartyNotificationMessageSender messageSender) {
        try {
            messageSender.sendMessage(message, serviceParameters);
        } catch (Exception e) {
            ThirdPartyNotificationType senderType = getThirdPartyNotificationServiceType(messageSender);
            logger.error("Error sending notification to " + senderType.name(), e);
            throw new NotificationServiceException("Error sending notification to " + senderType.name(), e);
        }
    }

    protected boolean isServiceConfigured(ThirdPartyNotificationType senderType) {
        return messageSenders != null ? messageSenders.stream().anyMatch(messageSender -> senderType.getNotificationMessageSenderClass().isInstance(messageSender)) : false;
    }

}
