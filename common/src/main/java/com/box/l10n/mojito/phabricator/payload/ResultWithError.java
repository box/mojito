package com.box.l10n.mojito.phabricator.payload;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResultWithError<FiledsT> {

    @JsonProperty("error_code")
    String errorCode;

    @JsonProperty("error_info")
    String errorInfo;

    Result<FiledsT> result;

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

    public Result<FiledsT> getResult() {
        return result;
    }

    public void setResult(Result<FiledsT> result) {
        this.result = result;
    }
}
