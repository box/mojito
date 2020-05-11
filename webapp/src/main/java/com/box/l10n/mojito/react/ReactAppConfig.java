package com.box.l10n.mojito.react;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class ReactAppConfig {

    @JsonUnwrapped
    ReactStaticAppConfig reactStaticAppConfig;

    ReactUser user;

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
}
