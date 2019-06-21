package com.box.l10n.mojito.smartling.response;

import com.box.l10n.mojito.smartling.request.UploadContextObject;

public class UploadContextResponse extends BaseResponse {

    UploadContextObject response;

    public UploadContextObject getResponse() {
        return response;
    }

    public void setResponse(UploadContextObject response) {
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
