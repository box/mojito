package com.box.l10n.mojito.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "third_party_text_unit",
    indexes = {
      @Index(
          name = "UK__THIRD_PARTY_TEXT_UNIT__TM_TEXT_UNIT_ID",
          columnList = "tm_text_unit_id",
          unique = true)
    })
@NamedEntityGraph(
    name = "ThirdPartyTextUnit.legacy",
    attributeNodes = {@NamedAttributeNode("asset"), @NamedAttributeNode("tmTextUnit")})
public class ThirdPartyTextUnit extends AuditableEntity {

  @Column(name = "third_party_id")
  String thirdPartyId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "asset_id",
      foreignKey = @ForeignKey(name = "FK__THIRD_PARTY_TEXT_UNIT__ASSET__ID"))
  Asset asset;

  @OneToOne(fetch = FetchType.LAZY)
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
