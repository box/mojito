package com.box.l10n.mojito.react;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReactAppConfig {

    @Autowired
    LinkConfig link;

    @Autowired
    LoginConfig login;

    @Autowired
    RepositoryStaticsConfig repositoryStaticsConfig;

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

    public RepositoryStaticsConfig getRepositoryStaticsConfig() {
        return repositoryStaticsConfig;
    }

    public void setRepositoryStaticsConfig(RepositoryStaticsConfig repositoryStaticsConfig) {
        this.repositoryStaticsConfig = repositoryStaticsConfig;
    }
}
