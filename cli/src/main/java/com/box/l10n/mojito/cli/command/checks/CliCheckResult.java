package com.box.l10n.mojito.cli.command.checks;

public class CliCheckResult {

    private final boolean successful;
    private final String notificationText;
    private final boolean hardFail;

    public CliCheckResult(boolean successful, String notificationText, boolean hardFail) {
        this.successful = successful;
        this.notificationText = notificationText;
        this.hardFail = hardFail;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getNotificationText() {
        return notificationText;
    }

    public boolean isHardFail() {
        return hardFail;
    }
}
