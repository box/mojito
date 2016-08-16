package com.box.l10n.mojito.boxsdk;

/**
 * @author jaurambault
 */
public class BoxSDKServiceException extends Exception {

    public BoxSDKServiceException(String message) {
        super(message);
    }

    public BoxSDKServiceException(String message, Throwable cause) {
        super(message, cause);
    }

}
