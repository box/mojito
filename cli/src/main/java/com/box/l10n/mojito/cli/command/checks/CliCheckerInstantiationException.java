package com.box.l10n.mojito.cli.command.checks;

public class CliCheckerInstantiationException extends RuntimeException {

    public CliCheckerInstantiationException(String message, Exception e){
        super(message, e);
    }
}
