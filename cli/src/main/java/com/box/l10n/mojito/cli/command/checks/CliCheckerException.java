package com.box.l10n.mojito.cli.command.checks;

public class CliCheckerException extends RuntimeException {

    private boolean hardFail;

    public CliCheckerException(String message, boolean hardFail, Exception e) {
        super(message, e);
        this.hardFail = hardFail;
    }

    public boolean isHardFail() {
        return hardFail;
    }
}
