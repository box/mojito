package com.box.l10n.mojito.phabricator;

public class PhabricatorException extends RuntimeException {

    public PhabricatorException(String message) {
        super(message);
    }

    public PhabricatorException(String message, Throwable cause) {
        super(message, cause);
    }
}
