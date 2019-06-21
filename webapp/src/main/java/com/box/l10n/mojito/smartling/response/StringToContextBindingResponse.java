package com.box.l10n.mojito.smartling.response;

import com.box.l10n.mojito.smartling.request.StringToContextBindingObject;

public class StringToContextBindingResponse extends BaseResponse {

    StringToContextBindingObject response;

    public StringToContextBindingObject getResponse() {
        return response;
    }

    public void setResponse(StringToContextBindingObject response) {
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
