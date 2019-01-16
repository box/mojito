package com.box.l10n.mojito.service.gitblame;

import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.GitBlame;
import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonView;

import java.util.Set;

public class GitBlameWithUsage {

    @JsonView(View.GitBlameWithUsage.class)
    Set<String> usages;

    @JsonView(View.GitBlameWithUsage.class)
    String textUnitName;

    @JsonView(View.GitBlameWithUsage.class)
    Long tmTextUnitId;

    @JsonView(View.GitBlameWithUsage.class)
    Long assetTextUnitId;

    @JsonView(View.GitBlameWithUsage.class)
    String content;

    @JsonView(View.GitBlameWithUsage.class)
    String comment;

    @JsonView(View.GitBlameWithUsage.class)
    GitBlame gitBlame;

    @JsonView(View.GitBlameWithUsage.class)
    Branch branch;

    public Set<String> getUsages() {
        return usages;
    }

    public void setUsages(Set<String> usages) {
        this.usages = usages;
    }

    public String getTextUnitName() {
        return textUnitName;
    }

    public void setTextUnitName(String textUnitName) {
        this.textUnitName = textUnitName;
    }

    public Long getTmTextUnitId() {
        return tmTextUnitId;
    }

    public void setTmTextUnitId(Long tmTextUnitId) {
        this.tmTextUnitId = tmTextUnitId;
    }

    public Long getAssetTextUnitId() {
        return assetTextUnitId;
    }

    public void setAssetTextUnitId(Long assetTextUnitId) {
        this.assetTextUnitId = assetTextUnitId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public GitBlame getGitBlame() {
        return gitBlame;
    }

    public void setGitBlame(GitBlame gitBlame) {
        this.gitBlame = gitBlame;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }
}
