package com.box.l10n.mojito.rest.client.exception;

/**
 * @author wyau
 */
public class RestClientException extends Exception {

    public RestClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public RestClientException(String message) {

        super(message);
    }
}
