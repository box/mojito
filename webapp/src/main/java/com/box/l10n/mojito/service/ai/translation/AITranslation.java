package com.box.l10n.mojito.service.ai.translation;

import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import java.time.ZonedDateTime;

public class AITranslation {

  TMTextUnit tmTextUnit;
  Long localeId;
  String translation;
  String contentMd5;
  TMTextUnitVariant.Status status;
  boolean includedInLocalizedFile;
  ZonedDateTime createdDate;
  String comment;

  public TMTextUnit getTmTextUnit() {
    return tmTextUnit;
  }

  public void setTmTextUnit(TMTextUnit tmTextUnit) {
    this.tmTextUnit = tmTextUnit;
  }

  public Long getLocaleId() {
    return localeId;
  }

  public void setLocaleId(Long localeId) {
    this.localeId = localeId;
  }

  public String getTranslation() {
    return translation;
  }

  public void setTranslation(String translation) {
    this.translation = translation;
  }

  public TMTextUnitVariant.Status getStatus() {
    return status;
  }

  public void setStatus(TMTextUnitVariant.Status status) {
    this.status = status;
  }

  public boolean isIncludedInLocalizedFile() {
    return includedInLocalizedFile;
  }

  public void setIncludedInLocalizedFile(boolean includedInLocalizedFile) {
    this.includedInLocalizedFile = includedInLocalizedFile;
  }

  public ZonedDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(ZonedDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public String getContentMd5() {
    return contentMd5;
  }

  public void setContentMd5(String content_md5) {
    this.contentMd5 = content_md5;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }
}
