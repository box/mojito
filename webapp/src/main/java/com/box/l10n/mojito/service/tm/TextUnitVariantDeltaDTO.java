package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.service.delta.DeltaType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Objects;

/** @author garion */
public class TextUnitVariantDeltaDTO implements TextUnitVariantDelta {
  @JsonIgnore Long textUnitVariantId;
  String textUnitName;
  String bcp47Tag;
  String content;
  DeltaType deltaType;

  public TextUnitVariantDeltaDTO(
      Long textUnitVariantId,
      String textUnitName,
      String bcp47Tag,
      String content,
      String deltaType) {
    this.textUnitVariantId = textUnitVariantId;
    this.textUnitName = textUnitName;
    this.bcp47Tag = bcp47Tag;
    this.content = content;
    this.deltaType = DeltaType.valueOf(deltaType);
  }

  public Long getTextUnitVariantId() {
    return textUnitVariantId;
  }

  public void setTextUnitVariantId(Long textUnitVariantId) {
    this.textUnitVariantId = textUnitVariantId;
  }

  public String getTextUnitName() {
    return textUnitName;
  }

  public void setTextUnitName(String textUnitName) {
    this.textUnitName = textUnitName;
  }

  public String getBcp47Tag() {
    return bcp47Tag;
  }

  public void setBcp47Tag(String bcp47Tag) {
    this.bcp47Tag = bcp47Tag;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public DeltaType getDeltaType() {
    return deltaType;
  }

  public void setDeltaType(DeltaType deltaType) {
    this.deltaType = deltaType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TextUnitVariantDeltaDTO that = (TextUnitVariantDeltaDTO) o;
    return Objects.equal(getTextUnitVariantId(), that.getTextUnitVariantId())
        && Objects.equal(getTextUnitName(), that.getTextUnitName())
        && Objects.equal(getBcp47Tag(), that.getBcp47Tag())
        && Objects.equal(getContent(), that.getContent())
        && getDeltaType() == that.getDeltaType();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        getTextUnitVariantId(), getTextUnitName(), getBcp47Tag(), getContent(), getDeltaType());
  }
}
