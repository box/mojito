package com.box.l10n.mojito.react;

import nu.validator.htmlparser.annotation.Auto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
public class ReactAppConfig {

    @Autowired
    LinkConfig link;

    @Autowired
    LoginConfig login;

    @Autowired
    SLAConfig slaConfig;

    public LinkConfig getLink() {
        return link;
    }

    public void setLink(LinkConfig link) {
        this.link = link;
    }

    public LoginConfig getLogin() {
        return login;
    }

    public void setLogin(LoginConfig login) {
        this.login = login;
    }

    public SLAConfig getSlaConfig() {
        return slaConfig;
    }

    public void setSlaConfig(SLAConfig slaConfig) {
        this.slaConfig = slaConfig;
    }
}
