package com.box.l10n.mojito.service.drop;

/**
 *
 * @author jaurambault
 */
public class ImportDropException extends Exception {

    public ImportDropException(String message) {
        super(message);
    }

    ImportDropException(String message, Throwable t) {
        super(message, t);
    }

}
