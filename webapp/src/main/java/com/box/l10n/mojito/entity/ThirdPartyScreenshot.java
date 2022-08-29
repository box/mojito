package com.box.l10n.mojito.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

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

  @OneToOne
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
