package com.box.l10n.mojito.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "third_party_screenshot",
    indexes = {
      @Index(
          name = "UK__THIRD_PARTY_SCREENSHOT__THIRD_PARTY_ID",
          columnList = "third_party_id",
          unique = true)
    })
public class ThirdPartyScreenshot extends AuditableEntity {

  @Column(name = "third_party_id")
  String thirdPartyId;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "screenshot_id",
      foreignKey = @ForeignKey(name = "FK__THIRD_PARTY_SCREENSHOT__SCREENSHOT__ID"))
  Screenshot screenshot;

  public String getThirdPartyId() {
    return thirdPartyId;
  }

  public void setThirdPartyId(String thirdPartyId) {
    this.thirdPartyId = thirdPartyId;
  }

  public Screenshot getScreenshot() {
    return screenshot;
  }

  public void setScreenshot(Screenshot screenshot) {
    this.screenshot = screenshot;
  }
}
