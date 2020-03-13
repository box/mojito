package com.box.l10n.mojito.react;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class ReactAppConfig {

    @JsonUnwrapped
    ReactStaticAppConfig reactStaticAppConfig;

    ReactUser user;

    String locale;

    boolean ict;

    String csrfToken;

    String contextPath;

    public ReactAppConfig(ReactStaticAppConfig reactStaticAppConfig, ReactUser reactUser) {
        this.reactStaticAppConfig = reactStaticAppConfig;
        this.user = reactUser;
    }

    public ReactStaticAppConfig getReactStaticAppConfig() {
        return reactStaticAppConfig;
    }

    public ReactUser getUser() {
        return user;
    }

    public void setReactStaticAppConfig(ReactStaticAppConfig reactStaticAppConfig) {
        this.reactStaticAppConfig = reactStaticAppConfig;
    }

    public void setUser(ReactUser user) {
        this.user = user;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public boolean isIct() {
        return ict;
    }

    public void setIct(boolean ict) {
        this.ict = ict;
    }

    public String getCsrfToken() {
        return csrfToken;
    }

    public void setCsrfToken(String csrfToken) {
        this.csrfToken = csrfToken;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }
}
