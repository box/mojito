package com.box.l10n.mojito.smartling;

public class SmartlingClientException extends RuntimeException {
    public SmartlingClientException(String message) {
        super(message);
    }

    public SmartlingClientException(Throwable cause) {
        super(cause);
    }

    public SmartlingClientException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
