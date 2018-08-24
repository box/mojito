package com.box.l10n.mojito.rest.textunit;

import com.box.l10n.mojito.rest.cli.GitInfo;

public class GitInfoForTextUnit {
    Long textUnitId;
    UserGitInfo  userGitInfo;


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
