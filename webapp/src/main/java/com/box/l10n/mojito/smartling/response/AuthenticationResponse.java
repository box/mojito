package com.box.l10n.mojito.smartling.response;

import com.box.l10n.mojito.smartling.request.AuthenticationObject;

public class AuthenticationResponse extends BaseResponse {
    private AuthenticationObject response;

    public AuthenticationObject getResponse() {
        return this.response;
    }

    public void setResponse(AuthenticationObject response) {
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
