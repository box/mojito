package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

/**
 * Thrown if an integrity check failed
 *
 * @author aloison
 */
public class IntegrityCheckException extends RuntimeException {

    public IntegrityCheckException(String message) {
        super(message);
    }

    public IntegrityCheckException(String message, Throwable cause) {
        super(message, cause);
    }

}
