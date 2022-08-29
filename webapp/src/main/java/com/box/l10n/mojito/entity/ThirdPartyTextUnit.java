package com.box.l10n.mojito.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(
    name = "third_party_text_unit",
    indexes = {
      @Index(
          name = "UK__THIRD_PARTY_TEXT_UNIT__TM_TEXT_UNIT_ID",
          columnList = "tm_text_unit_id",
          unique = true)
    })
public class ThirdPartyTextUnit extends AuditableEntity {

  @Column(name = "third_party_id")
  String thirdPartyId;

  @ManyToOne(optional = false)
  @JoinColumn(
      name = "asset_id",
      foreignKey = @ForeignKey(name = "FK__THIRD_PARTY_TEXT_UNIT__ASSET__ID"))
  Asset asset;

  @OneToOne
  @JoinColumn(
      name = "tm_text_unit_id",
      foreignKey = @ForeignKey(name = "FK__THIRD_PARTY_TEXT_UNIT__TM_TEXT_UNIT__ID"))
  TMTextUnit tmTextUnit;

  public String getThirdPartyId() {
    return thirdPartyId;
  }

  public void setThirdPartyId(String thirdPartyId) {
    this.thirdPartyId = thirdPartyId;
  }

  public TMTextUnit getTmTextUnit() {
    return tmTextUnit;
  }

  public void setTmTextUnit(TMTextUnit tmTextUnit) {
    this.tmTextUnit = tmTextUnit;
  }

  public Asset getAsset() {
    return asset;
  }

  public void setAsset(Asset asset) {
    this.asset = asset;
  }
}
