package com.box.l10n.mojito.notifications.service.slack;

public enum SlackParameters {

    USERNAME("slack_username"),
    ATTACHMENT_COLOR("slack_attachment_color");

    private String paramKey;

    SlackParameters(String paramKey){
        this.paramKey = paramKey;
    }

    public String getParamKey() {
        return paramKey;
    }
}
