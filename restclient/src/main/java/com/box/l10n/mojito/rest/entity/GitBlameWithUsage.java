package com.box.l10n.mojito.rest.entity;

import java.util.Set;

public class GitBlameWithUsage {

    Set<String> usages;

    String textUnitName;

    String pluralForm;

    Long tmTextUnitId;

    Long assetTextUnitId;

    String content;

    String comment;

    GitBlame gitBlame;

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

    public String getPluralForm() {
        return pluralForm;
    }

    public void setPluralForm(String pluralForm) {
        this.pluralForm = pluralForm;
    }
}
