package com.box.l10n.mojito.smartling.response;

import com.box.l10n.mojito.smartling.request.SourceStringsObject;

public class SourceStringsResponse extends BaseResponse {
    SourceStringsObject response;

    public SourceStringsObject getResponse() {
        return response;
    }

    public void setResponse(SourceStringsObject response) {
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
