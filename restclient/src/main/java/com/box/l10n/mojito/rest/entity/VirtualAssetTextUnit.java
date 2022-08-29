package com.box.l10n.mojito.rest.entity;

/** @author jaurambault */
public class VirtualAssetTextUnit {

  String name;
  String content;
  String comment;

  String pluralForm;
  String pluralFormOther;

  Boolean doNotTranslate;

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

  public Boolean getDoNotTranslate() {
    return doNotTranslate;
  }

  public void setDoNotTranslate(Boolean doNotTranslate) {
    this.doNotTranslate = doNotTranslate;
  }
}
