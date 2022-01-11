package com.box.l10n.mojito.notifications.service;

public class NotificationServiceException extends RuntimeException {

    public NotificationServiceException(String message) {
        super(message);
    }

    public NotificationServiceException(String message, Throwable t) {
        super(message, t);
    }
}
