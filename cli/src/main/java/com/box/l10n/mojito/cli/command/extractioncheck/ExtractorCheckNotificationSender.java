package com.box.l10n.mojito.cli.command.extractioncheck;

import com.box.l10n.mojito.cli.command.checks.CliCheckResult;

import java.util.List;

public interface ExtractorCheckNotificationSender {
    void sendFailureNotification(List<CliCheckResult> failures, boolean hardFail) throws ExtractorCheckNotificationSenderException;

    void sendChecksSkippedNotifications() throws ExtractorCheckNotificationSenderException;
}
