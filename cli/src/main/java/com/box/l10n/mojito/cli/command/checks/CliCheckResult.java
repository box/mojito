package com.box.l10n.mojito.cli.command.checks;

public class CliCheckResult {

    private final boolean successful;
    private final String notificationText;
    private final boolean hardFail;
    private final String checkName;

    public CliCheckResult(boolean successful, String notificationText, boolean hardFail, String checkName) {
        this.successful = successful;
        this.notificationText = notificationText;
        this.hardFail = hardFail;
        this.checkName = checkName;
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

    public String getCheckName() {
        return checkName;
    }
}
