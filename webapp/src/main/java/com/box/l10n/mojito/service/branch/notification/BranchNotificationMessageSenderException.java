package com.box.l10n.mojito.service.branch.notification;

import com.box.l10n.mojito.slack.SlackClientException;

public class BranchNotificationMessageSenderException extends Throwable {
    public BranchNotificationMessageSenderException(SlackClientException sce) {
        super(sce);
    }
}
