package com.box.l10n.mojito.rest.entity;

/**
 *
 * @author jaurambault
 */
public class ErrorMessage {

    String type;
    String message;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
