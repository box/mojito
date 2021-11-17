package com.box.l10n.mojito.cli.command.checks;

public class CliCheckResult {

    private final boolean hardFail;
    private final String checkName;
    private boolean successful = true;
    private String notificationText = "";

    public CliCheckResult(boolean hardFail, String checkName) {
        this.hardFail = hardFail;
        this.checkName = checkName;
    }

    public CliCheckResult(boolean successful, boolean hardFail, String checkName) {
        this.successful = successful;
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

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public void setNotificationText(String notificationText) {
        this.notificationText = notificationText;
    }
}
