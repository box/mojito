package com.box.l10n.mojito.cli.command.checks;

public class CliCheckerException extends RuntimeException {

    public CliCheckerException(String message, Exception e) {
        super(message, e);
    }
}
