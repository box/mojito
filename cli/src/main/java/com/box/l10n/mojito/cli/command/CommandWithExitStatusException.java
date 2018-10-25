package com.box.l10n.mojito.cli.command;

/**
 *
 * @author jeanaurambault
 */
public class CommandWithExitStatusException extends CommandException {

    int exitCode = 0;
    
    public CommandWithExitStatusException(int exitCode) {
        super("Exit with special exit code");
        this.exitCode = exitCode;
    }

    public int getExitCode() {
        return exitCode;
    }
}
