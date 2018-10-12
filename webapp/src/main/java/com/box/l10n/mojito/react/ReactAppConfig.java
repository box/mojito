package com.box.l10n.mojito.react;

import nu.validator.htmlparser.annotation.Auto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
public class ReactAppConfig {

    @Autowired
    OpengrokConfig opengrok;

    @Autowired
    LoginConfig login;

    public OpengrokConfig getOpengrok() {
        return opengrok;
    }

    public void setOpengrok(OpengrokConfig opengrok) {
        this.opengrok = opengrok;
    }

    public LoginConfig getLogin() {
        return login;
    }

    public void setLogin(LoginConfig login) {
        this.login = login;
    }
}
