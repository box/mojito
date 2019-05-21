package com.box.l10n.mojito.smartling.response;

import com.box.l10n.mojito.smartling.request.FilesObject;

public class FilesResponse extends BaseResponse {
    FilesObject response;

    public FilesObject getResponse() {
        return response;
    }

    public void setResponse(FilesObject response) {
        this.response = response;
    }

    public String getErrorMessage() {
        return formatErrors(this.response.getErrors());
    }

    public Boolean isSuccessResponse() {
        return getIsSuccessResponse(this.response);
    }

    public Boolean isAuthenticationErrorResponse() {
        return getIsAuthenticationErrorResponse(this.response);
    }
}
