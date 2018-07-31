package com.box.l10n.mojito.rest.textunit;

import com.box.l10n.mojito.entity.GitBlame;

import java.util.List;

public class GitBlameWithUsages {

    Long tmTextUnitId;
    List<String> usages;

    GitBlame gitBlame;

    public Long getTmTextUnitId() {
        return tmTextUnitId;
    }

    public void setTmTextUnitId(Long tmTextUnitId) {
        this.tmTextUnitId = tmTextUnitId;
    }

    public List<String> getUsages() {
        return usages;
    }

    public void setUsages(List<String> usages) {
        this.usages = usages;
    }

    public GitBlame getGitBlame() {
        return gitBlame;
    }

    public void setGitBlame(GitBlame gitBlame) {
        this.gitBlame = gitBlame;
    }
}
