package com.box.l10n.mojito.phabricator.conduit.payload;

public class ResponseWithError<FiledsT> {

    String errorMessage;

    Response<FiledsT> response;

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Response<FiledsT> getResponse() {
        return response;
    }

    public void setResponse(Response<FiledsT> response) {
        this.response = response;
    }
}
