package com.box.l10n.mojito.rest.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 *
 * @author jeanaurambault
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HttpClientErrorJson {

    String status;
    String message;
    String exception;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

}
