package com.box.l10n.mojito.service.gitblame;

import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.GitBlame;
import com.box.l10n.mojito.entity.Screenshot;
import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonView;

import java.util.HashSet;
import java.util.Set;

public class GitBlameWithUsage {

    @JsonView(View.GitBlameWithUsage.class)
    Set<String> usages;

    @JsonView(View.GitBlameWithUsage.class)
    String textUnitName;

    @JsonView(View.GitBlameWithUsage.class)
    String pluralForm;

    @JsonView(View.GitBlameWithUsage.class)
    Long tmTextUnitId;

    @JsonView(View.GitBlameWithUsage.class)
    Long assetId;

    @JsonView(View.GitBlameWithUsage.class)
    Long assetTextUnitId;

    @JsonView(View.GitBlameWithUsage.class)
    String thirdPartyTextUnitId;

    @JsonView(View.GitBlameWithUsage.class)
    String content;

    @JsonView(View.GitBlameWithUsage.class)
    String comment;

    @JsonView(View.GitBlameWithUsage.class)
    GitBlame gitBlame;

    @JsonView(View.GitBlameWithUsage.class)
    Branch branch;

    @JsonView(View.GitBlameWithUsage.class)
    Set<Screenshot> screenshots = new HashSet<>();

    @JsonView(View.GitBlameWithUsage.class)
    boolean isVirtual;

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
    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }
    public Long getAssetTextUnitId() {
        return assetTextUnitId;
    }

    public void setAssetTextUnitId(Long assetTextUnitId) {
        this.assetTextUnitId = assetTextUnitId;
    }

    public String getThirdPartyTextUnitId() {
        return thirdPartyTextUnitId;
    }

    public void setThirdPartyTextUnitId(String thirdPartyTextUnitId) {
        this.thirdPartyTextUnitId = thirdPartyTextUnitId;
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

    public Set<Screenshot> getScreenshots() {
        return screenshots;
    }

    public void setScreenshots(Set<Screenshot> screenshots) {
        this.screenshots = screenshots;
    }

    public String getPluralForm() {
        return pluralForm;
    }

    public void setPluralForm(String pluralForm) {
        this.pluralForm = pluralForm;
    }
    public boolean getVirtual() {
        return isVirtual;
    }

    public void setVirtual(boolean isVirtual) {
        this.isVirtual = isVirtual;
    }
}
