package com.box.l10n.mojito.service.drop;

/**
 *
 * @author jaurambault
 */
public class CancelDropException extends Exception {

    public CancelDropException(String message) {
        super(message);
    }

    CancelDropException(String message, Throwable t) {
        super(message, t);
    }

}
