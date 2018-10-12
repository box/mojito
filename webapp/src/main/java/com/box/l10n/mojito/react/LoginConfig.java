package com.box.l10n.mojito.react;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LoginConfig {

    @Autowired
    OAuth2Config oauth2;

    public OAuth2Config getOauth2() {
        return oauth2;
    }

    public void setOauth2(OAuth2Config oauth2) {
        this.oauth2 = oauth2;
    }
}
