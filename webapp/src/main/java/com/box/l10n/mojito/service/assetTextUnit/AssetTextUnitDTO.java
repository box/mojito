package com.box.l10n.mojito.service.assetTextUnit;

public class AssetTextUnitDTO {

    String name;
    String content;
    String comment;
    Long pluralFormId;
    String pluralFormOther;
    String md5;
    boolean doNotTranslate;

    public AssetTextUnitDTO(String name, String content, String comment, Long pluralFormId, String pluralFormOther, String md5, boolean doNotTranslate) {
        this.name = name;
        this.content = content;
        this.comment = comment;
        this.pluralFormId = pluralFormId;
        this.pluralFormOther = pluralFormOther;
        this.md5 = md5;
        this.doNotTranslate = doNotTranslate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Long getPluralFormId() {
        return pluralFormId;
    }

    public void setPluralFormId(Long pluralFormId) {
        this.pluralFormId = pluralFormId;
    }

    public String getPluralFormOther() {
        return pluralFormOther;
    }

    public void setPluralFormOther(String pluralFormOther) {
        this.pluralFormOther = pluralFormOther;
    }

    public boolean isDoNotTranslate() {
        return doNotTranslate;
    }

    public void setDoNotTranslate(boolean doNotTranslate) {
        this.doNotTranslate = doNotTranslate;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }
}
