package com.box.l10n.mojito.ltm.merger;

import com.google.common.collect.ImmutableMap;

import java.util.Date;
import java.util.Objects;

public class BranchStateTextUnit {

    Long id;
    String md5;
    Date createdDate;

    String name;
    String source;
    String comments;
    String pluralForm;
    String pluralFormOther;

    ImmutableMap<Branch, BranchData> branchToBranchDatas = ImmutableMap.of();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getPluralForm() {
        return pluralForm;
    }

    public void setPluralForm(String pluralForm) {
        this.pluralForm = pluralForm;
    }

    public String getPluralFormOther() {
        return pluralFormOther;
    }

    public void setPluralFormOther(String pluralFormOther) {
        this.pluralFormOther = pluralFormOther;
    }

    public ImmutableMap<Branch, BranchData> getBranchToBranchDatas() {
        return branchToBranchDatas;
    }

    public void setBranchToBranchDatas(ImmutableMap<Branch, BranchData> branchToBranchDatas) {
        this.branchToBranchDatas = branchToBranchDatas;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BranchStateTextUnit that = (BranchStateTextUnit) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(md5, that.md5) &&
                Objects.equals(createdDate, that.createdDate) &&
                Objects.equals(name, that.name) &&
                Objects.equals(source, that.source) &&
                Objects.equals(comments, that.comments) &&
                Objects.equals(pluralForm, that.pluralForm) &&
                Objects.equals(pluralFormOther, that.pluralFormOther) &&
                Objects.equals(branchToBranchDatas, that.branchToBranchDatas);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, md5, createdDate, name, source, comments, pluralForm, pluralFormOther, branchToBranchDatas);
    }
}
