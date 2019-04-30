package com.box.l10n.mojito.smartling.request;

public class AuthenticationObject extends BaseObject {

    private AuthenticationData data;

    public void setData(AuthenticationData data) {
        this.data = data;
    }

    public AuthenticationData getData() {
        return this.data;
    }

}
