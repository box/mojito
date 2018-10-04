package com.box.l10n.mojito.react;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
public class ReactAppConfig {

    @Autowired
    OpengrokConfig opengrok;

    public OpengrokConfig getOpengrok() {
        return opengrok;
    }

    public void setOpengrok(OpengrokConfig opengrok) {
        this.opengrok = opengrok;
    }
}
