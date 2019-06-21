package com.box.l10n.mojito.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.*;

/**
 * Entity used to store the mapping between screenshots stored by third party and screenshots stored by mojito
 *
 * @author chidinmae
 */
@Entity
@Table(name = "third_party_text_unit_screenshot",
        indexes = {@Index(name = "THIRD_PARTY_TEXT_UNIT_SCREENSHOT__THIRD_PARTY_TEXT_UNIT_ID", columnList = "third_party_text_unit_id")})
public class ThirdPartyTextUnitScreenshot extends AuditableEntity {

    @Column(name = "third_party_screenshot_id")
    private String thirdPartyScreenshotId;

    @ManyToOne
    @JoinColumn(name = "third_party_text_unit_id", foreignKey = @ForeignKey(name = "FK__THIRD_PARTY_TEXT_UNIT_SCREENSHOT__THIRD_PARTY_TEXT_UNIT__ID"), nullable = false)
    private ThirdPartyTextUnit thirdPartyTextUnit;

    @OneToOne
    @JoinColumn(name = "screenshot_text_unit_id", foreignKey = @ForeignKey(name = "FK__THIRD_PARTY_TEXT_UNIT_SCREENSHOT__SCREENSHOT_TEXT_UNIT__ID"), nullable = false)
    private ScreenshotTextUnit screenshotTextUnit;

    public String getThirdPartyScreenshotId() {
        return thirdPartyScreenshotId;
    }

    public void setThirdPartyScreenshotId(String thirdPartyScreenshotId) {
        this.thirdPartyScreenshotId = thirdPartyScreenshotId;
    }

    public ThirdPartyTextUnit getThirdPartyTextUnit() {
        return thirdPartyTextUnit;
    }

    public void setThirdPartyTextUnit(ThirdPartyTextUnit thirdPartyTextUnit) {
        this.thirdPartyTextUnit = thirdPartyTextUnit;
    }

    public ScreenshotTextUnit getScreenshotTextUnit() {
        return this.screenshotTextUnit;
    }

    public void setScreenshotTextUnit(ScreenshotTextUnit screenshotTextUnit) {
        this.screenshotTextUnit = screenshotTextUnit;
    }

}
