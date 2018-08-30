package com.box.l10n.mojito.rest.entity;

public class GitInfoForTextUnit {

    Long textUnitId;

    UserGitInfo userGitInfo;

    public Long getTextUnitId() {
        return textUnitId;
    }

    public void setTextUnitId(Long textUnitId) {
        this.textUnitId = textUnitId;
    }

    public UserGitInfo getUserGitInfo() {
        return userGitInfo;
    }

    public void setUserGitInfo(UserGitInfo userGitInfo) {
        this.userGitInfo = userGitInfo;
    }

}
