package com.box.l10n.mojito.service.thirdparty;

public class ThirdPartyTMSGlossarySyncException extends RuntimeException {

    public ThirdPartyTMSGlossarySyncException(String message) {
        super(message);
    }

    public ThirdPartyTMSGlossarySyncException(String message, Throwable t) {
        super(message, t);
    }
}
