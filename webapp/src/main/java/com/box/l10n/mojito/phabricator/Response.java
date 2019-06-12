package com.box.l10n.mojito.phabricator;


import com.fasterxml.jackson.annotation.JsonProperty;

public class Response {

    @JsonProperty("error_code")
    String errorCode;

    @JsonProperty("error_info")
    String errorInfo;

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorInfo() {
        return errorInfo;
    }

    public void setErrorInfo(String errorInfo) {
        this.errorInfo = errorInfo;
    }
}
