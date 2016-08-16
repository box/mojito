package com.box.l10n.mojito.cli.command;

/**
 * Unrecoverable execution error that need to be shown to the user and lead to
 * existing the CLI with an error code.
 *
 * <p>
 * The message will be displayed to the end user so it should be simple and
 * doesn't contain to technical information. More information will be added in
 * the logs.
 *
 * @author jaurambault
 */
public class CommandException extends Exception {

    public CommandException(Throwable cause) {
        super(cause);
    }

    public CommandException(String message) {
        super(message);
    }

    public CommandException(String message, Throwable cause) {
        super(message, cause);
    }

}
